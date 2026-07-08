# Implementation Guidelines: App Scoping & the Platform App

Companion to `.plan/specs/url-grammar.md`. Grounded in the existing code (see evidence inline).
This is the "how to build it" layer for the `_`/`api`/bare grammar.

## Two orthogonal axes (do not conflate)
1. **Mount scope** — the app's *nature*: how much context it needs to do its job. Declared by
   the app. Determines URL depth + injected context.
2. **Availability** — *which schemas* may use it and *who* may see it. Deployment/schema config
   + role gating. (Today: all apps global + ungated — `StaticFileMapper.java:8-9` TODO already
   anticipates per-app permissions.)

### Mount scopes (4)
| Scope | URL | Injected context | Example |
|-------|-----|------------------|---------|
| global | `/_{app}` | — (server) | central, admin dashboard |
| schema | `/{schema}/_{app}` | schema | settings, schema editor, a portal |
| table | `/{schema}/{table}/_{app}` | schema, table | a chart bound to a table |
| record | `/{schema}/{table}/row/{id}/_{app}` | schema, table, row | a custom record viewer |

The schema's **front app** is the special case that owns the *bare* namespace (no `_`); default
front app = the platform. A front-capable app dual-mounts: bare when front, `_name` when not.

## 1. App manifest (NEW — does not exist today)
Apps are currently bare directories with no metadata (`ServeStaticFile.java:53-188`; no manifest
found anywhere). Introduce a manifest at each bundle root, e.g. `molgenis-app.json`:
```json
{
  "name": "my-portal",
  "scope": "schema",        // global | schema | table | record
  "front": true,            // front-capable? (may own a schema's bare namespace)
  "label": "My Portal",
  "minRole": "Viewer",      // optional: gate SERVING, not just menu visibility
  "provides": []            // for the platform bundle: extra _names it owns (see §5)
}
```
Backend scans `public_html/apps/*` and `custom-app/*` at startup → in-memory **app registry**
`Map<name, AppEntry{dir, scope, front, minRole, provides}>`. This is the one new backend concept;
everything else reuses existing wiring.

## 2. Backend resolver (extend ServeStaticFile / StaticFileMapper)
Decide only *which bundle's index.html to serve* + *what context to inject*; the SPA routes the
rest. After schema resolution, given the in-schema path:
```
if seg[0] == "api":                  -> existing API router (unchanged)
find deepest "_{app}" segment in the path:
  present at a depth whose scope matches registry[app].scope, and user role >= minRole:
                                      -> serve registry[app].dir/index.html
                                         inject context to the LEFT of that segment
  present but unknown / wrong scope / forbidden:  -> 404 / 403
  absent:                             -> serve the schema's FRONT app index.html
                                         (default = platform); inject {schema}; SPA parses
                                         the bare {table}/{verb}/row/{id} tail itself
```
Notes:
- "deepest `_app`" lets table/record apps live under bare table paths without ambiguity.
- bundle lookup = the same "does a dir exist" check done today, now keyed by the registry.
- 404 for unknown `_name` keeps typos out of the SPA; bare unknown = front app shows not-found.

## 3. Context injection contract (NEW — pick ONE, use everywhere)
Today schema is *guessed*: Vue apps issue a GraphQL `_schema` query relative to their URL
(`apps/tables/src/App.vue:40-68`); Nuxt apps read `route.params` / `runtimeConfig`
(`apps/ui/.../[schema]/index.vue:23-50`, `apps/catalogue/nuxt.config.ts:17-29`). With dual-mount
an app can't reliably guess its base. So the backend must **inject** the mount context.

Recommended: backend injects a JSON blob into the served `index.html` before the app bundle:
```html
<script>window.__MOLGENIS_APP__ = {
  "schema": "petstore", "table": "Pet", "row": null,
  "app": "my-portal", "basePath": "/petstore/Pet/_my-portal"
}</script>
```
- App reads `schema/table/row` from this instead of parsing `window.location` — dual-mount works.
- GraphQL endpoint stays relative: `${basePath}/../graphql` resolves to `/{schema}/graphql`
  (the existing relative-graphql convention keeps working — `MolgenisSession.vue:88-91`).
- For Nuxt SPA: surface the same object via `runtimeConfig.public`. For catalogue (SSR): the
  server already has the full request URL + `basePath`, so it reads scope from the route — no
  injection needed, same field names.

This is the single most important new contract: **apps receive scope, they do not infer it.**

## 4. Reuse the menu — only the hrefs change
The per-schema menu already exists as role-gated JSON (`SchemaMenu.java:13-66`): items are
`{label, href, role, key, submenu}`. Keep the structure; update `href` values to the new grammar
(`"Pet/list"`, `"_settings"`). The existing `role` field already does per-item *visibility*; for
real *access* gating add `minRole` enforcement at serve time (§1–2), closing the
`StaticFileMapper.java:8-9` TODO. Default menu lives in `apps/settings/.../MenuManager.vue:62-89`.

## 5. The platform app = ONE app
Consolidate the internal view-apps (tables→explorer, settings, schema, pages, tasks, up/download)
into a **single Nuxt app** = the platform. `_settings`, `_schema`, etc. are route groups inside
it, not separate bundles. One build, one `index.html`, client-side routing.
- Its manifest: `{ "name": "platform", "front": true, "default": true,
  "provides": ["_settings","_schema","_pages","_tasks","_explore"] }`.
- Registry maps every `provides` name AND the bare/default front slot → the platform bundle.
- Catalogue and custom apps remain **separate** bundles with their own manifests; the resolver
  treats them identically — the rule doesn't care whether code is one bundle or many.

So: **one platform app + N independent apps (catalogue, externals, table widgets)**, unified by
the resolver — not one app for everything.

## 6. Front-app config (reuse landing machinery)
- Per-schema **front app** setting (which app owns bare). Default = platform. Store as a schema
  setting alongside `menu`; seed from / coexist with the existing first-menu-item redirect
  (`MolgenisWebservice.java:211-255`). With a front app owning bare, `/{schema}/` *serves* the
  front app directly (the redirect-to-first-menu-item can be retired for schemas with a front app).
- Keep `LANDING_PAGE` (db-level) for server root `/` (`MolgenisWebservice.java:96-107`).

## 7. Custom/external apps — what changes vs today
- Storage: unchanged (`custom-app/` + `CUSTOM_APP_PATH`).
- NEW: each carries a manifest (§1) → gets a scope, front-capability, and optional role gate.
- NEW: per-schema **enablement** — a schema setting listing which custom apps are available there
  (closes "all apps are global" — `ServeStaticFile.java:103-188`). Resolver checks enablement
  before serving a custom `_name` under a schema.
- A front-capable external app (e.g. a project portal) can own a schema's bare namespace exactly
  like catalogue — that is the "others get short paths too" mechanism.

## Build order (maps to migration plan)
1. App registry + manifest scan + the resolver (§1–2) — backend, no UI change yet.
2. Context-injection contract (§3) — backend inject + a shared frontend reader.
3. Platform app consolidation (§5) + dual-mount — the big frontend lift, rides bootstrap→Nuxt.
4. Front-app setting (§6) + generalize catalogue off its nginx whitelist.
5. Custom-app manifest + per-schema enablement + role gating (§7, §4).

## Open
- Manifest filename/format (`molgenis-app.json` vs `package.json` field vs DB row).
- Whether `provides` is explicit or the platform is simply the fallback for any unclaimed `_name`.
- Role-gating granularity: per-app `minRole` vs full per-schema-role app ACLs.
