# Plan: URL Grammar Migration

Target grammar: see `.plan/specs/url-grammar.md` (v2 folded in 2026-07-05). Decisions:
`.plan/decisions.md`. Superseded background (primer, standalone v2 review, app-scoping
implementation guide) is in `.plan/plans/archive/`.
**No redirect/back-compat layer for APP urls** — the bootstrap→Nuxt rewrite changes those
anyway, so the grammar change rides that rewrite as a clean cutover. Data APIs
(`/{schema}/api/…`) are unchanged; bare `/{schema}/graphql` is kept as a permanent alias.

## Current state (as found, with evidence)
- Backend (Javalin): schema = **first** path segment; static apps resolve by rewriting the
  first segment to `apps/` and serving `/public_html/apps/{app}/`
  (`backend/molgenis-emx2-webapi/.../web/ServeStaticFile.java:23-188`,
  `StaticFileMapper.java:19-25`, `MolgenisWebservice.java:330-336`).
- APIs mostly walled under `/{schema}/api/…` — BUT three bare in-schema endpoints exist
  (verified 2026-07-05): `/{schema}/graphql` (`GraphqlApi.java:62`), `/{schema}/theme.css`
  (`BootstrapThemeService.java:38`), `/{schema}/sitemap.xml` (`SiteMapService.java:17`),
  plus app-relative `…/{app}/graphql` variants (`GraphqlApi.java:47,67`). The spec rules on
  each: graphql → canonical `api/graphql` + permanent bare alias + reserved word; dotted
  files → permanent well-knowns; app-relative graphql dies with the cutover.
- Bootstrap SPAs (tables, settings, schema, pages, tasks, updownload…): `/{schema}/{app}#/{route}`,
  Vue3+Vite, hash routing. **Being rewritten into the Nuxt platform app — URL change rides this.**
- `ui` (Nuxt SPA): app-first nesting `/apps/ui/{schema}/…`, base `'/apps/ui/'`. Becomes the
  platform. Record URLs currently smuggle composite keys as `?keys=<JSON>`
  (`apps/ui/app/pages/[schema]/[table]/[entity].vue:32-40`) — replaced by the `PrimaryKey`
  path encoding (spec).
- `catalogue` (Nuxt SSR): own frontend, `[catalogue]` param == schema; root via nginx whitelist
  (`apps/catalogue/sample-nginx.conf`). **The front-app proof-of-concept we generalize.**
- Custom apps: `custom-app/` dir, served at `/apps/{name}` or `/{schema}/{name}` (`CUSTOM_APP_PATH`).
- Row-key serialization already exists in the backend: `PrimaryKey`
  (`molgenis-emx2-rdf/.../PrimaryKey.java`) — sorted `name=value&…`, parse +
  GraphQL-filter conversion built in. Promote to core (Phase 0).

Key enabler already present: an app exists iff its bundle dir exists; tables have no dir and
fall through. We change the fallthrough to serve the **front app** (default: explorer) index,
and gate apps behind the `_` prefix.

## The one rule (implemented at every depth)
`api` → APIs · `_name` → an app/platform surface (stable) · bare → the front app's content.
Resolution is **leftmost `_name` at a valid scope boundary** (never "deepest" — app-internal
`_` segments belong to the app); front-capable app tails re-enter the table grammar. Reserved
words: `api`, `graphql`, the explorer verbs; dotted segments are well-known files. See spec
"Resolution algorithm".

## Implementation notes (absorbed from the archived app-scoping guide)
- **App manifest (NEW):** apps are bare directories today, no metadata anywhere. Introduce
  `molgenis-app.json` at each bundle root:
  `{ "name", "scope": "global|schema|table|record", "front": bool, "assets": ["_nuxt"],
  "label", "minRole", "provides": [] }`. Backend scans `public_html/apps/*` and
  `custom-app/*` at startup → in-memory registry `Map<name, AppEntry>`. The one new backend
  concept; everything else reuses existing wiring.
- **Resolver:** decides only *which bundle's index.html to serve* + *what context to inject*;
  the SPA routes the rest. Unknown `_name` → 404 (keeps typos out of the SPA); bare unknown →
  front app shows not-found.
- **Context injection contract (pick ONE, use everywhere):** backend injects
  `window.__MOLGENIS_APP__ = { schema, table, row, app, basePath, graphqlEndpoint }` into the
  served index.html (Nuxt SPA: same object via `runtimeConfig.public`; catalogue SSR reads it
  from the route — same field names). **Apps receive scope, they never infer it** — today
  schema is guessed from the URL (`apps/tables/src/App.vue:40-68`,
  `apps/ui/.../[schema]/index.vue:23-50`), which dual-mount breaks. `graphqlEndpoint` =
  `/{schema}/api/graphql`; the app-relative `…/{app}/graphql` convention retires.
- **Menu: reuse, only hrefs change.** Per-schema role-gated menu JSON already exists
  (`SchemaMenu.java:13-66`); update `href` values to the new grammar (`"Pet/list"`,
  `"_settings"`). Add `minRole` enforcement at SERVE time (closes the
  `StaticFileMapper.java:8-9` TODO). Default menu: `apps/settings/.../MenuManager.vue:62-89`.
- **The platform = ONE app.** Consolidate tables/settings/schema/pages/tasks/up-download into
  a single Nuxt app; `_settings`, `_schema` etc. are route groups inside it, declared via
  manifest `provides`. Catalogue and custom apps remain separate bundles; the resolver treats
  them identically.
- **Front-app config: reuse landing machinery.** Per-schema setting stored alongside `menu`;
  coexists with / retires the first-menu-item redirect (`MolgenisWebservice.java:211-255`).
  `LANDING_PAGE` (db-level) stays for server root `/` (`MolgenisWebservice.java:96-107`).
- **Custom apps:** storage unchanged (`custom-app/` + `CUSTOM_APP_PATH`); NEW manifest,
  per-schema **enablement** setting, and serve-time role gate.

## Phase 0 — The `_` boundary + name validation + app scope (prerequisite) [backend]
- Resolver: leftmost rule per spec; at each depth `_`-prefixed segment → app at that scope;
  `api`/`graphql`/dotted → per spec; bare → front app / table / verb / row-key.
- App registry + manifest scan (implementation notes above).
- **Promote `PrimaryKey` to core** + shared link-builder util; fix the legacy `+`
  space-escaping (align `%20`) and the ARRAY-key gap (#4944) at promotion time. Decide the
  single-key-shorthand emission rule here.
- Validation: reject **schema** names == `api` or starting `_`; reject **table** names ==
  `api`, `graphql`, or a view verb (`list|card|graph|aggregate|create|row|edit`). Pin the
  existing name regexes (no `_`-prefix / dots possible) with regression tests.
- JUnit: resolver-per-depth tests (incl. leftmost vs app-internal `_` segments);
  name-validation tests (red-green); `PrimaryKey` URL roundtrip incl. `=`/`&` in values.
- Acceptance → spec rows: resolver rows, name-validation rows, `PrimaryKey` rows, "App
  declares its scope…".

## Phase 1 — Platform explorer as default front app at bare root [frontend + backend]
- Rebuild the unified explorer with `app.baseURL` supporting **dual mount**: bare
  `/{schema}/…` when it is the front, and `/{schema}/_explore/…` when it is not. Unique
  asset prefix via manifest `assets` (e.g. `_explore-assets`) + **relative links** so one
  build serves both.
- Backend fallback: `/{schema}/{seg}` that is not `api`/`graphql`/dotted/`_*` and has no app
  bundle → serve the front app (default explorer) index (replaces the `isUi` special-case).
- Add `/{schema}/api/graphql` canonical route now (tiny), so the new explorer never learns
  the bare alias.
- Explorer reads schema + table from injected context + path (history mode, no hash).
- Tests: backend fallthrough resolution; manifest asset-prefix serving; frontend path-parse +
  relative-base link spec.
- Acceptance → spec rows: "Front app owns bare…", "Front-capable app dual-mounts…",
  "manifest `assets` prefixes…", "api/graphql canonical…".

## Phase 2 — Table grammar (verbs + row/) [frontend]
- Implement `/{table}/list|card|graph|aggregate|create` and `/{table}/row/{key}[/edit]`.
- Bare `/{table}` → `/{table}/list`. `{table}` = identifier, never display name.
- Record links use the core `PrimaryKey` encoding (+ single-key shorthand); remove `?keys=`.
- Tests: router specs per verb; a row whose key equals a verb word still resolves under
  `row/`; composite-key URL roundtrip; shorthand roundtrip.
- Acceptance → spec rows: all `/{schema}/{table}/…` rows + key-encoding rows.

## Phase 2b — Table- & record-level app mounting + context injection [backend + frontend]
- Resolver: `/{schema}/{table}/_{app}` and `/{schema}/{table}/row/{key}/_{app}` resolve to a
  registered app at that scope (after verb/`row`/`edit` checks), else 404; same grammar under
  `_explore` (recursion rule).
- Inject context per the contract (implementation notes): table-level app gets
  `schema`+`table`; record-level gets `schema`+`table`+`row`.
- Tests: resolver picks app vs verb at each depth; recursion under `_explore`;
  context-injection unit test.
- Acceptance → spec rows: "table-level app mounts…", "record-level app mounts…", "Table
  grammar applies under `_explore`…".

## Phase 3 — Front-app mechanism + generalize catalogue [backend + frontend]
- Per-schema **front app** setting (implementation notes: reuse landing machinery).
- `/{schema}/` and the bare namespace route to the configured front app; platform surfaces
  stay at stable `_*` paths; `api` always to backend.
- **Replace catalogue's nginx whitelist** with the syntactic `_`/`api`/well-known boundary.
  **Deliverable: a reference proxy config** (nginx + traefik snippet implementing the rule) —
  that artifact is what actually replaces catalogue's hand-maintained file at member sites.
- Make catalogue (and any custom front app) dual-mount like the explorer (manifest `assets`:
  `["_nuxt"]`).
- Tests: front-app resolution; platform reachable via `_*` under a non-platform front.
- Acceptance → spec rows: "Front app owns bare; platform reachable via `_*`/`api`…".

## Phase 4 — Host mode & network (vanity domains) [backend + frontend]
- Host→schema config map; backend resolves schema from `Host` when no path schema; identical
  grammar at root. Apps emit schema-relative links so one build works at `/{schema}/…` and `/…`.
- Pin the precedence rules (spec "Schema resolution"): one host = one schema; global system
  routes win at any root; `/api` on a vanity host = schema API; well-knowns
  (`robots.txt`/`favicon.ico`/`.well-known/*`) at every root.
- Recommended: record-URL **content negotiation** (html → front app; rdf → 303 to
  `api/rdf/{table}/{key}` — a string rewrite, since URL and IRI share the `PrimaryKey`
  encoding).
- Tests: host-resolver unit test; precedence tests; relative-base link generation; conneg
  303 mapping.
- Acceptance → spec rows: "Host maps to schema…", "Vanity host exposes ONLY…", "Global system
  routes…", well-known row, conneg row.

## Phase 5 — Custom-app registration [backend]
- On registration, read declared scope + front-capability + assets; mount as `_{name}` at
  that scope (or allow as a front app); enforce registry uniqueness — name must not equal a
  system route, built-in `_name`, same-scope app, or any declared asset prefix.
- Serve from `custom-app/{name}`; per-schema enablement check; inject scope-appropriate
  context (Phase 2b); `minRole` gate at serve time.
- Acceptance → spec rows: "App declares its scope…", "Custom app cannot shadow…".

## Risks / notes
- **Trickiest engineering = dual-mount base URL** (Phase 1/3): a built SPA's base is
  build-time; solved by relative links + manifest asset prefixes + the backend front-app
  fallthrough. This is the client-routing/asset-loading class that needs a browser/e2e pass —
  opt-in, batch it.
- No redirects for app URLs: communicate the break alongside the bootstrap→Nuxt rollout.
  (Data APIs unchanged; bare graphql aliased forever; if any embedded/bookmarked app URLs
  matter, add a short-lived shim — owner's call.)
- Freeze the explorer's verb set before Phase 2 ships (changing it later breaks links).
- Catalogue is the front-app POC; Phase 3 turns its bespoke nginx into the general rule.
- `PrimaryKey` promotion touches the RDF module's IRIs — keep IRI output byte-identical while
  fixing the `+` quirk, or version the change consciously (RDF consumers may have stored IRIs).
