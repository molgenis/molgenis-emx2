# Spec: EMX2 URL Grammar — v2 (review revision)

Status: DESIGN PROPOSAL — v2 of `.plan/specs/url-grammar.md`, produced by design review
2026-07-05. Not yet owner-approved; v1 stays authoritative until this is accepted (then fold
back / replace). Companion docs (`url-grammar-primer.md`, `plans/url-grammar-migration.md`,
`specs/app-scoping-implementation.md`) get the deltas listed at the end.

---

## What the review found (summary of changes vs v1)

The core design is sound and survives review: the `api` / `_name` / bare rule, schema-first,
front apps with dual-mount, and scope-by-depth all hold up. v2 keeps all of it. What changes:

1. **A false premise, verified against code.** v1 says "ALL machine APIs live under
   `/{schema}/api/…` — unchanged". Not true today: GraphQL is at bare `/{schema}/graphql`
   (`GraphqlApi.java:62`), the theme at `/{schema}/theme.css` (`BootstrapThemeService.java:38`),
   the sitemap at `/{schema}/sitemap.xml` (`SiteMapService.java:17`). Under the v1 grammar these
   bare segments would be swallowed by the front-app fallthrough — silently breaking every
   GraphQL client. §3 below resolves this (dotted names become "well-known files"; `graphql`
   becomes a reserved word with `api/graphql` as the canonical path).
2. **Row identity was undefined — and it's load-bearing.** EMX2 has composite primary keys; the
   current ui app smuggles them past the URL as `?keys=<JSON>` (`[entity].vue:32-40`). That
   breaks the "citable record URL" goal and the one-segment assumption in the resolution rules.
   §5 defines the encoding.
3. **The resolver rule in app-scoping §2 ("deepest `_` segment") is wrong.** An app whose
   *internal* client route contains a `_` segment (`/{schema}/_pages/edit/_draft`) would
   misresolve to a nonexistent app `_draft` → 404. §6 replaces it with *leftmost-at-a-valid-
   boundary*, plus a recursion rule so record-level apps still work under a `_`-mounted explorer.
4. **Bare-mounted front apps would 404 their own assets.** A Nuxt app fronting a schema requests
   `/{schema}/_nuxt/…`; the resolver sees an unknown `_name` → 404. The manifest now declares
   asset prefixes (§7). This generalizes the plan's one-off `buildAssetsDir` trick.
5. **Host mode had unpinned collisions.** What happens to `/_login`, `/api`, `/robots.txt`, and
   the server's *other* schemas on a vanity domain was unspecified. §4 pins all four.
6. **The actual goal — many domains behaving as one network — had no section.** v1 treats host
   mode as a vanity feature. §9 makes the network story first-class: uniform grammar as the
   federation protocol, canonical record URLs, content negotiation for machine clients.
7. **Good news from validation code:** table names already *cannot* start with `_` or contain
   `.` or `-` (`Constants.java:131` — `^[a-zA-Z][a-zA-Z0-9 _]{0,30}$`, no leading `_` possible,
   letters/digits/spaces/underscore only). So the `_` boundary and the dotted-file rule are
   *already structurally guaranteed*; new validation shrinks to rejecting `api`, `graphql`, and
   the view verbs as table names.
8. **Honest framing.** v1 sells "no reserved list". In truth there is a *small frozen
   vocabulary*: `api`, the view verbs, `row`, `edit`, `graphql`, plus dotted well-knowns. v2
   says so plainly — one sigil + one tiny frozen word list beats pretending the list is empty.
9. **Pinned: URLs carry identifiers, not display names.** Table names may contain spaces; their
   camelCase identifier may not. The ui app already routes by identifier. URLs use identifiers
   (`/petstore/PetStoreUsers/list`, never `/petstore/Pet%20store%20users/list`). Schema segment
   stays the schema name, as the backend routes today.
10. **Registry hygiene:** a custom app may not shadow a built-in `_name` or a system route —
    uniqueness enforced at registration.

---

## 1. Goals (owner) — one addition

- Shortest sensible **data** paths; `/{schema}/{table}/list` is the model endpoint.
- Schema-first (the tenant is the primary segment), consistent across all apps.
- One **platform** app owns the short paths; other apps can front a schema too.
- Keep serving user-supplied custom app code.
- Let a user put their schema on their own URL (vanity domain).
- Catalogue stays an SSR app on its own frontend.
- **NEW (was implicit): many EMX2 instances on many domains behave as one network** — any
  record on any member instance has a stable, constructable, citable URL, so catalogues and
  aggregators can deep-link across members with zero per-member configuration (§9).

## 2. The one rule (+ the honest fine print)

At every level, one syntactic boundary decides everything:

```
api    → machine APIs
_name  → an app / platform surface   (stable URL, structurally collision-free)
bare   → the schema's FRONT APP's content (user data: tables, rows, …)
```

Fine print — the small frozen vocabulary that rides along:
- **Reserved words** (may not be table names): `api`, `graphql` (§3), and the explorer verbs
  `list card graph aggregate create row edit`. Enforced at table create/rename. Frozen before
  Phase 2 ships; additions after that only at depths where they can't collide (§5).
- **Dotted segments are never tables** (the name regex forbids `.`): a first segment containing
  a dot is a *well-known file* (`theme.css`, `sitemap.xml`) or 404. No fallthrough to apps.
- Leading `_` on tables is already impossible per the existing name regex — no new validation
  needed there, only a regression test that pins the regex.

## 3. Where today's bare in-schema endpoints land

| Today (verified) | v2 ruling |
|---|---|
| `/{schema}/graphql` | Canonical becomes `/{schema}/api/graphql` (add route). Bare path stays as a **permanent alias** — it's the single most-used client endpoint (pyclient, scripts, every frontend); `graphql` becomes a reserved table name so the alias can never be shadowed. Cheapest compat win in the whole design. |
| `/{schema}/{app}/graphql`, `apps/{app}/…` variants | Die with the app-URL cutover (they exist for app-relative calls; the context contract §8 replaces them). |
| `/{schema}/theme.css`, `/{schema}/sitemap.xml` | Stay bare forever as **well-known files** — dotted, so structurally collision-free. No move needed. |
| `/robots.txt`, `/favicon.ico`, `/.well-known/*` | Served at every root (path-mode server root AND every vanity-domain root). Required for the SEO/vanity goal; dotted/reserved, no collisions. |

## 4. Schema resolution — path mode and host mode, edges pinned

```
/{schema}/…        multi-schema server (path mode)
myproject.org/…    host→schema map; schema segment dropped (host mode)
```

Host-mode rules that v1 left open:

- **One host maps to exactly one schema.** The server's other schemas are *not* reachable under
  that host (otherwise `/{seg}` is ambiguous between "table of the mapped schema" and "another
  schema"). Other schemas live on their own host or on the canonical path-mode domain.
- **Global system routes win at any root.** `/_login`, `/_callback` (and the asset routes of
  whatever serves them) resolve *before* schema-app lookup, on every host. The global set is
  tiny and frozen; a schema app can never claim those names (§10 uniqueness).
- **`/api` on a vanity host = that schema's API.** The cross-schema API (`/api/graphql` over
  all schemas) is reachable only on the canonical path-mode host. One rule, no ambiguity.
- **Well-knowns at every root** per §3.
- Apps must emit **schema-relative links** so one build serves `/{schema}/…` and vanity `/…`
  (already required by dual-mount; host mode is the same discipline).

Top level (path mode) is unchanged from v1:

```
/                  server landing (configurable)
/api/…             cross-schema APIs
/_{app}/…          global apps (central, admin) + global custom apps
/_login /_callback reserved system routes
```
Reserved schema names: `api`, anything starting `_` (schema regex allows `-`, so also document
that hyphens are fine).

## 5. In-schema grammar and record identity

```
/{schema}/                        front app home
/{schema}/api/…                   machine APIs (canonical home of graphql from now on)
/{schema}/graphql                 permanent alias → api/graphql (reserved word)
/{schema}/{well-known.file}       theme.css, sitemap.xml
/{schema}/_{app}/…                schema-level app (stable, dual-mountable)
/{schema}/{table}/…               front-app content; for the platform front: the explorer
```

Explorer table grammar (unchanged shape, now with `{key}` defined):

```
/{schema}/{table}                 redirect → {table}/list
/{schema}/{table}/list|card|graph|aggregate    views
/{schema}/{table}/create          new-record form
/{schema}/{table}/_{app}          TABLE-level app (handed schema+table)
/{schema}/{table}/row/{key}       record detail — THE canonical, citable record URL
/{schema}/{table}/row/{key}/edit  edit record
/{schema}/{table}/row/{key}/_{app} RECORD-level app (handed schema+table+row)
```

**Record key encoding (`{key}`) — new, replaces `?keys=<JSON>`:**
- The backend ALREADY has a canonical row-key serialization: **`PrimaryKey`**
  (`molgenis-emx2-rdf/.../PrimaryKey.java`) — sorted `name=value` pairs joined by `&`,
  percent-escaped, with parse + GraphQL-filter conversion built in. **Promote it to core and
  reuse it here**, so explorer URLs and RDF row IRIs share ONE row-identity encoding — the
  content-negotiation 303 in §9 then maps key-for-key.
- Composite key: `/stats/Measurement/row/country=NL&year=2024`. (`=` and `&` are legal inside
  a path segment per RFC 3986; the escaper encodes them *within values*, so parsing is
  unambiguous.) Refs in keys flatten to their scalar reference names, as `PrimaryKey` does.
- Single-column key (the common case) gets the bare-value shorthand:
  `/patients/Patient/row/P0001` — no unescaped `=` present + table has one key column → the
  segment is that column's value. Link builders emit shorthand for single keys, pairs otherwise.
- The encoding is **structural** (key=1 columns only, no display templates): a citable URL can
  never change because someone edits a label. A table-level *rowLabel* (display concat of the
  key columns — see §8) is a worthwhile separate feature, but it must never feed the URL.
- One segment always → the resolution rules below stay metadata-free, and a key value equal to
  a verb word can never collide (it sits under `row/`).
- Adopting `PrimaryKey` for URLs is the moment to fix its two known quirks: the legacy `+`
  space-escaping (align on `%20`, see `IriGenerator` note) and no ARRAY-typed keys (#4944).
- Future *record* verbs (history, copy, …) can be added after `row/{key}/` safely: today a bare
  unknown segment there is 404, so additions only turn 404s into pages.

`{table}` is the table **identifier** (camelCase, no spaces) — as the ui app already routes.

## 6. Resolution algorithm (fixes the "deepest `_`" bug)

Given in-schema segments `s1/s2/…`, scan **left to right**; the first match wins:

```
1. s1 == "api"                → API router
2. s1 == "graphql"            → alias for api/graphql
3. s1 contains "."            → well-known file, else 404
4. s1 starts with "_"         → global system route (login/callback/asset prefixes) first;
                                else registry lookup at SCHEMA scope:
                                  known + enabled-for-schema + role ok → serve that app;
                                  the remaining tail belongs to the app (opaque), EXCEPT
                                  front-capable apps: their tail re-enters rule T below
                                unknown _name → 404 (keeps typos out of the SPA)
5. else s1 is a table         → the FRONT app owns it; if the front app is the explorer
                                (or any front-capable app using the table grammar),
                                parse the tail per rule T; else tail is the front app's
T. table tail:  verb            → explorer view
                row/{key}       → record; then optional  edit | _{recordApp}
                _{tableApp}     → registry lookup at TABLE scope (enabled, role ok)
                empty           → redirect to list
```

Why leftmost + scoped recursion instead of v1's "deepest `_` segment":
- Leftmost means an app's internal routes may contain `_` segments freely — the resolver never
  looks past the segment that selected the app.
- The recursion rule means the table grammar applies **wherever the explorer is mounted**:
  `/{schema}/_explore/Pet/row/1/_audit` resolves the record app exactly like
  `/{schema}/Pet/row/1/_audit` does when the platform is the front. One grammar, two mounts —
  which is the whole dual-mount promise.

## 7. Front app, dual-mount, and the asset-prefix fix

Unchanged from v1: exactly one bare namespace per schema, owned by the configured front app
(default = platform explorer); the platform stays reachable at `_*`/`api` under any front;
front-capable apps dual-mount (bare when front, `_name` when not); this generalizes
catalogue's hand-written nginx whitelist into one syntactic rule.

**New: manifests declare asset prefixes.** A bare-mounted SPA/SSR app requests its own build
assets (`/_nuxt/…`, `/_explore-assets/…`). Those are `_names` the resolver would otherwise 404.
The manifest lists them and the registry maps them to the owning bundle:

```json
{
  "name": "catalogue",
  "scope": "schema",
  "front": true,
  "assets": ["_nuxt"],
  "minRole": null
}
```

Rule of thumb: every front-capable app needs a **unique** asset prefix (the platform's
`/_explore-assets/` trick from the migration plan, promoted from workaround to manifest field),
so two apps' assets never contend for one prefix under the same schema.

## 8. App scopes and context injection (carried from v1, one amendment)

| Scope | Mounts at | Context injected |
|-------|-----------|------------------|
| global | `/_{app}` | — |
| schema | `/{schema}/_{app}` (or bare, if front) | schema |
| table | `/{schema}/{table}/_{app}` | schema, table |
| record | `/{schema}/{table}/row/{key}/_{app}` | schema, table, row |

Context is **injected, never inferred** (`window.__MOLGENIS_APP__` / runtimeConfig, per
app-scoping §3). Amendment: the injected blob's `graphqlEndpoint` points at
`/{schema}/api/graphql` — the old app-relative `…/{app}/graphql` convention (which the grammar
retires) is replaced by this field, so apps stop computing endpoints from their own URL.

**Related display feature (owner suggestion, 2026-07-05): table-level `rowLabel`.** Today the
"how do I show a row" default is computed per *ref column* (`Column.getRefLabelDefault`,
Column.java:584). Hoisting it to the table — a template defaulting to the concat of the key=1
columns, which refs *inherit* as their default refLabel and the explorer uses for record-page
titles/breadcrumbs/search results — is a clean normalization and pairs naturally with this
grammar. It is display-only by design: URLs and IRIs use the structural `PrimaryKey` encoding
(§5), so editing a rowLabel never breaks a citation. Track as its own small feature, not a
grammar dependency.

## 9. The network: many domains, one grammar (new section — the point of it all)

The grammar *is* the federation protocol. Because every EMX2 instance resolves the identical
grammar, a record's identity is just the tuple **(origin, schema, table, key)** — and its URL
is constructable by any peer without asking the member anything:

```
https://biobank-nl.org/Dataset/row/ds42          (host mode member)
https://emx2.umcg.nl/rare-disease/Patient/row/P7 (path mode member)
```

What this buys the network, concretely:

- **Aggregators need zero per-member config.** A network catalogue that harvests member
  metadata stores the tuple and emits deep links into members mechanically. Today that requires
  knowing each member's app layout (`/apps/ui/...`? nginx rewrite? hash route?); after this,
  the link format is a constant of the network.
- **Citable, stable record IRIs.** `/{schema}/{table}/row/{key}` never changes when a schema
  swaps its front app or when apps are renamed — bare data paths belong to the data, `_` paths
  to the plumbing. That's the property that makes the URL safe to print in a paper.
- **Content negotiation makes the same URL serve humans and machines** (recommended, Phase 4):
  `Accept: text/html` → the front app; `Accept: text/turtle` (etc.) → `303 See Other` to
  `/{schema}/api/rdf/{table}/{key}`. One IRI per record for FAIR/linked-data use across the
  network, no separate "semantic URL" to mint. (EMX2 already has the RDF API; this is a
  redirect rule, not a new subsystem.)
- **Network-wide search/indexing** falls out of per-schema `sitemap.xml` at stable locations
  plus vanity-domain roots that can serve `robots.txt` (§3).
- *Deferred, optional:* a machine-readable instance descriptor at `/.well-known/emx2`
  (version, public schemas) so network crawlers can discover members. Not needed for v1 of the
  network story; noted so the `.well-known` root stays reserved.

## 10. Registry & validation rules (consolidated)

- Table names: regex already blocks `_`-prefix, dots, hyphens. ADD: reject `api`, `graphql`,
  and the verb set `list|card|graph|aggregate|create|row|edit`. Pin the regex with a test.
- Schema names: reject `api` and `_`-prefix (regex already requires leading letter — pin it).
- App registration: name must not equal a global system route (`login`, `callback`), any
  built-in `_name`, another registered app at the same scope, or any declared asset prefix.
- Apps declare: `scope`, `front` (capability), `assets`, optional `minRole`; per-schema
  **enablement** decides where a custom app is reachable (app-scoping §7 unchanged).

## 11. Behavior contract — delta rows (v1 rows carry over unless amended here)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `/{schema}/api/graphql` added as canonical; bare `/{schema}/graphql` alias kept | backend GraphqlApi | TBD | — |
| `graphql` rejected as table name | metadata validation | TBD | — |
| Dotted first segment → well-known file (theme.css, sitemap.xml), never front app | resolver | TBD | — |
| `robots.txt`/`favicon.ico`/`.well-known/*` served at path-mode root AND vanity roots | resolver / proxy rule | TBD | — |
| Composite key encodes via `PrimaryKey` (sorted `name=value&…`) in ONE segment; single-key shorthand roundtrips; `=`/`&` inside values survive | explorer router + link builder + core `PrimaryKey` | TBD | — |
| Explorer row URL and RDF row IRI use the SAME key encoding | `PrimaryKey` (promoted to core) | TBD | — |
| `?keys=<JSON>` pattern removed from record URLs | explorer | TBD | — |
| Resolver picks LEFTMOST `_name` at a valid scope boundary; app-internal `_` segments never resolve | resolver | TBD | — |
| Table grammar (incl. `_app` mounts) applies under `_explore` same as under bare mount | resolver recursion | TBD | — |
| Front app's manifest `assets` prefixes serve its bundle when bare-mounted | resolver + manifest | TBD | — |
| Global system routes (`_login`, `_callback`) resolve on vanity hosts before schema apps | resolver | TBD | — |
| Vanity host exposes ONLY its mapped schema; `/api` there = schema API | host resolver | TBD | — |
| URLs use table identifiers, not display names | explorer link builder | TBD | — |
| Custom app cannot shadow built-in `_name` / system route / asset prefix | registration validation | TBD | — |
| Record URL content-negotiates: html → front app, rdf → 303 to api/rdf (optional) | backend | TBD | — |

## 12. Migration plan deltas (against `plans/url-grammar-migration.md`)

- **Phase 0** gains: promote `PrimaryKey` from the rdf module to core (+ fix `+` escaping and
  ARRAY-key gap #4944) + a shared link-builder util; reserved words `graphql` + verbs;
  leftmost resolver rule (replace "deepest"); pin the name-regex tests.
- **Phase 1** gains: manifest `assets` field + registry mapping (replaces the hard-coded
  `buildAssetsDir` note); `api/graphql` canonical route (tiny, do it here so the new explorer
  never learns the bare path).
- **Phase 3** gains: ship a **reference proxy config** (nginx + traefik snippet implementing
  the `_`/`api`/well-known rule) as a deliverable — that artifact is what actually replaces
  catalogue's hand-maintained file at member sites.
- **Phase 4 (host mode)** gains: precedence rules of §4, well-knowns at vanity roots, and
  (optional, recommended) record-URL content negotiation → this is the "network" phase; rename
  it from "vanity domains" to "host mode & network" to keep the goal visible.
- **Phase 5** gains: registration uniqueness checks (§10).

## 13. Open questions (narrowed from v1)

- ~~Key encoding: comma-join vs named pairs~~ — resolved 2026-07-05: reuse `PrimaryKey`
  (sorted `name=value&…`) + bare-value shorthand for single-column keys (§5). Remaining
  sub-question: is the shorthand emitted always, or only when the value can't be mistaken
  for a pair?
- Table-level `rowLabel` (display concat of key=1 columns, refs inherit) — endorsed as a
  separate feature (§8); scope/owner TBD, not a grammar blocker.
- Content negotiation scope: RDF only, or also JSON (`Accept: application/json` → api/json)?
- Front-app setting storage & manifest filename — carried over from v1 unchanged.
- `.well-known/emx2` instance descriptor — deferred; only the path reservation is decided.
- ~~How context is injected~~ — resolved: injected blob incl. `graphqlEndpoint` (§8).
- ~~Reserved set / collision story~~ — resolved: §2 fine print + §10.
