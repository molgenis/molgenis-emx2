# Spec: EMX2 URL Grammar

Status: DESIGN (not yet implemented). v2 — the 2026-07-05 review revision is folded in
(see `.plan/decisions.md` entry 2026-07-05; the standalone review doc and the primer live in
`.plan/plans/archive/`). Target grammar + guardrail for all routing work.
Tests are mostly planned (TBD) until the migration phases land.

## In plain language (start here if you're not a developer)
Every dataset, table, and record in MOLGENIS lives at a web address. Today those addresses
are inconsistent: depending on which part of the product you use, the same data appears at
differently shaped addresses — some long and cluttered with technical prefixes, some needing
hand-written server configuration to look nice.

This design gives the whole product **one simple address scheme** that reads like a path to
your data: first the dataset collection, then the table, then the record —

```
molgenis.org/petstore/Pet/list        browse the Pet table
molgenis.org/petstore/Pet/row/123     open record 123
molgenis.org/petstore/_settings       the settings tool (tools start with "_")
biobanks.org/Dataset/row/ds42         the same idea on an institute's own domain
```

The rule behind it: a segment named `api` is for machines, a name starting with `_` is a
tool or app, and **everything else is your data**. That's the whole grammar.

Why it matters:
- **Links become short, readable, and permanent** — safe to bookmark, share, or cite in a
  publication. They don't break when the software around the data changes.
- **Your data on your own domain** becomes a simple setting instead of custom server work.
- **Many MOLGENIS servers behave as one network**: because every server uses the same
  scheme, a catalogue on one server can link straight to a record on another — across
  organizations and domains — without any coordination.
- **Custom apps keep working**, and can never accidentally clash with your tables, because
  apps and data are kept apart by that one `_` rule.

Everything below is the technical contract that makes this precise.

## Goals (owner)
- Shortest sensible **data** paths; `/{schema}/{table}/list` is the model endpoint.
- Schema-first (the tenant is the primary segment), consistent across all apps.
- One **platform** app owns the short paths; other apps can front a schema too.
- Keep serving user-supplied custom app code.
- Let a user put their schema on their own URL (vanity domain).
- Catalogue stays an SSR app on its own frontend.
- **Many EMX2 instances on many domains behave as one network** — any record on any member
  instance has a stable, constructable, citable URL, so catalogues and aggregators can
  deep-link across members with zero per-member configuration (see "The network" below).

## The one rule (+ the honest fine print)
At every level, one syntactic boundary decides everything:

```
api    → machine APIs
_name  → an app / platform surface   (stable URL, structurally collision-free)
bare   → the schema's FRONT APP's content (user data: tables, rows, …)
```

`_` already means "framework, not your data" in EMX2 (`_login`, `_callback`, `_nuxt`); we
extend that one convention. A future built-in app can therefore NEVER collide with an
existing user table — the namespaces are separated by syntax, forever.

Fine print — the small frozen vocabulary that rides along:
- **Reserved words** (may not be table names): `api`, `graphql` (see next section), and the
  explorer verbs `list card graph aggregate create row edit`. Enforced at table
  create/rename. Frozen before Phase 2 ships; later additions only at depths where they
  can't collide (see key encoding notes).
- **Dotted segments are never tables** (the name regex forbids `.`): a first segment
  containing a dot is a *well-known file* (`theme.css`, `sitemap.xml`) or 404. No
  fallthrough to apps.
- Table names already *cannot* start with `_` or contain `.`/`-`
  (`Constants.java:131` — `^[a-zA-Z][a-zA-Z0-9 _]{0,30}$`), so the `_` boundary and the
  dotted-file rule are structurally guaranteed today; only pin the regex with a
  regression test.

## Where today's bare in-schema endpoints land
Verified against code — these are NOT under `/api/` today and would otherwise be swallowed
by the front-app fallthrough:

| Today (verified) | Ruling |
|---|---|
| `/{schema}/graphql` (`GraphqlApi.java:62`) | Canonical becomes `/{schema}/api/graphql` (add route). Bare path stays as a **permanent alias** — it's the single most-used client endpoint (pyclient, scripts, every frontend); `graphql` becomes a reserved table name so the alias can never be shadowed. |
| `/{schema}/{app}/graphql`, `apps/{app}/…` variants (`GraphqlApi.java:47,67`) | Die with the app-URL cutover; the context-injection contract replaces app-relative endpoint computation. |
| `/{schema}/theme.css` (`BootstrapThemeService.java:38`), `/{schema}/sitemap.xml` (`SiteMapService.java:17`) | Stay bare forever as **well-known files** — dotted, so structurally collision-free. No move needed. |
| `/robots.txt`, `/favicon.ico`, `/.well-known/*` | Served at every root (path-mode server root AND every vanity-domain root). Required for the SEO/vanity goal. |

## Schema resolution (two modes, identical grammar after)
```
/{schema}/…        multi-schema server (path mode)
myproject.org/…    host→schema map; schema segment dropped (host mode)
```

Host-mode rules:
- **One host maps to exactly one schema.** The server's other schemas are *not* reachable
  under that host (otherwise `/{seg}` is ambiguous between "table of the mapped schema" and
  "another schema"). Other schemas live on their own host or on the canonical path-mode domain.
- **Global system routes win at any root.** `/_login`, `/_callback` (and the asset routes of
  whatever serves them) resolve *before* schema-app lookup, on every host. The global set is
  tiny and frozen; a schema app can never claim those names (registry uniqueness below).
- **`/api` on a vanity host = that schema's API.** The cross-schema API is reachable only on
  the canonical path-mode host.
- **Well-knowns at every root** (`robots.txt`, `favicon.ico`, `.well-known/*`).
- Apps must emit **schema-relative links** so one build serves `/{schema}/…` and vanity `/…`
  (already required by dual-mount; host mode is the same discipline).

### Top level (no schema, path mode)
```
/                  server landing (configurable)
/api/…             cross-schema APIs (graphql, …)
/_{app}/…          global apps (central, admin) + global custom apps
/_login /_callback reserved system routes
```
Reserved **schema** names (first segment): `api` and anything starting with `_` (the schema
regex allows `-`; hyphens are fine).

## In-schema grammar
```
/{schema}/                        front app home
/{schema}/api/…                   machine APIs (canonical home of graphql from now on)
/{schema}/graphql                 permanent alias → api/graphql (reserved word)
/{schema}/{well-known.file}       theme.css, sitemap.xml
/{schema}/_{app}/…                schema-level app (stable, dual-mountable)
/{schema}/{table}/…               front-app content; for the platform front: the explorer
```

### Table grammar (the explorer / platform front app)
```
/{schema}/{table}                 redirect → {table}/list
/{schema}/{table}/list|card|graph|aggregate    views
/{schema}/{table}/create          new-record form
/{schema}/{table}/_{app}          TABLE-level app (handed schema+table)
/{schema}/{table}/row/{key}       record detail — THE canonical, citable record URL
/{schema}/{table}/row/{key}/edit  edit record
/{schema}/{table}/row/{key}/_{app} RECORD-level app (handed schema+table+row)
```
View verbs and `row` are the explorer's fixed vocabulary. User records live under
`row/{key}`, so verbs never collide with user data. `_`-prefixed segments are apps at that
scope; they never collide with verbs.

`{table}` is the table **identifier** (camelCase, no spaces) — as the ui app already routes;
never the display name (which may contain spaces).

### Record key encoding (`{key}`) — replaces `?keys=<JSON>`
- The backend ALREADY has a canonical row-key serialization: **`PrimaryKey`**
  (`molgenis-emx2-rdf/.../PrimaryKey.java`) — sorted `name=value` pairs joined by `&`,
  percent-escaped, with parse + GraphQL-filter conversion built in. **Promote it to core and
  reuse it here**, so explorer URLs and RDF row IRIs share ONE row-identity encoding — the
  content-negotiation 303 (network section) then maps key-for-key.
- Composite key: `/stats/Measurement/row/country=NL&year=2024`. (`=` and `&` are legal inside
  a path segment per RFC 3986; the escaper encodes them *within values*, so parsing is
  unambiguous.) Refs in keys flatten to their scalar reference names, as `PrimaryKey` does.
- Single-column key (the common case) gets the bare-value shorthand:
  `/patients/Patient/row/P0001` — no unescaped `=` present + table has one key column → the
  segment is that column's value. Link builders emit shorthand for single keys, pairs
  otherwise.
- The encoding is **structural** (key=1 columns only, no display templates): a citable URL can
  never change because someone edits a label. A table-level *rowLabel* (display concat of the
  key columns — see "App scope & context") is a worthwhile separate feature, but it must never
  feed the URL.
- One segment always → the resolution rules stay metadata-free, and a key value equal to a
  verb word can never collide (it sits under `row/`).
- Future *record* verbs (history, copy, …) can be added after `row/{key}/` safely: today a
  bare unknown segment there is 404, so additions only turn 404s into pages.
- Adopting `PrimaryKey` for URLs is the moment to fix its two known quirks: the legacy `+`
  space-escaping (align on `%20`, see `IriGenerator` note) and no ARRAY-typed keys (#4944).

## Resolution algorithm
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

Why leftmost + scoped recursion (NOT "deepest `_` segment"):
- Leftmost means an app's internal routes may contain `_` segments freely
  (`/{schema}/_pages/edit/_draft` belongs to `_pages`) — the resolver never looks past the
  segment that selected the app.
- The recursion rule means the table grammar applies **wherever the explorer is mounted**:
  `/{schema}/_explore/Pet/row/1/_audit` resolves the record app exactly like
  `/{schema}/Pet/row/1/_audit` does when the platform is the front. One grammar, two
  mounts — the dual-mount promise.

## Front app (who owns the bare namespace)
There is exactly **one bare namespace per schema**, owned by the schema's configured
**front app**. Default front app = the **platform** (its data explorer). A schema may set a
different front app (catalogue, a custom portal). "Let others have short paths" = any app can
be the front app *for its schema* — not two apps short at once.

Whatever the front app, the **platform stays reachable** via `_`-paths and `api`. This is the
generalization of catalogue's hand-maintained nginx whitelist (`/api`, `/apps`, specific names
→ backend; rest → catalogue) into one syntactic rule that needs no per-deployment config.

```
Platform is front:   /{schema}/Pet/list   (data)   /{schema}/_settings   /{schema}/_schema
Catalogue is front:  /{schema}/Dataset/ds42 (data) /{schema}/_settings   /{schema}/_explore/Pet/list
                     ^ same _settings, api, _* regardless of front app
```

**Front-capable apps must dual-mount:** run at bare root when they are the front, and at
their `_name` when they are not. Catalogue already does this (root via nginx, or
`/apps/catalogue`), so "front-capable" = "supports dual mounting" — a known pattern.

**Manifests declare asset prefixes.** A bare-mounted SPA/SSR app requests its own build
assets (`/_nuxt/…`, `/_explore-assets/…`). Those are `_names` the resolver would otherwise
404. The manifest lists them and the registry maps them to the owning bundle:

```json
{
  "name": "catalogue",
  "scope": "schema",
  "front": true,
  "assets": ["_nuxt"],
  "minRole": null
}
```
Every front-capable app needs a **unique** asset prefix (the platform's `/_explore-assets/`
approach, promoted from workaround to manifest field), so two apps' assets never contend for
one prefix under the same schema.

## App scope & context (schema / table / record)
Scope is declared by the app and expressed by **mount depth**; depth == the context injected.
`_name` marks an app at every depth uniformly.

| Scope | Mounts at | Context injected | Examples |
|-------|-----------|------------------|----------|
| global | `/_{app}` | — (server) | central, admin |
| schema | `/{schema}/_{app}` (or bare, if front) | schema | settings, schema editor, explorer, catalogue, custom portal |
| table | `/{schema}/{table}/_{app}` | schema, table | a chart bound to one table |
| record | `/{schema}/{table}/row/{key}/_{app}` | schema, table, row | a custom record viewer/editor |

Context is **injected, never inferred** (`window.__MOLGENIS_APP__` / runtimeConfig — see the
context-injection contract in the migration plan). The injected blob's `graphqlEndpoint`
points at `/{schema}/api/graphql` — the old app-relative `…/{app}/graphql` convention is
retired; apps stop computing endpoints from their own URL.

**Related display feature (owner, 2026-07-05): table-level `rowLabel`.** Today the "how do I
show a row" default is computed per *ref column* (`Column.getRefLabelDefault`,
Column.java:584). Hoisting it to the table — a template defaulting to the concat of the key=1
columns, which refs *inherit* as their default refLabel and the explorer uses for record-page
titles/breadcrumbs/search results — is a clean normalization and pairs naturally with this
grammar. Display-only by design: URLs and IRIs use the structural `PrimaryKey` encoding, so
editing a rowLabel never breaks a citation. Track as its own small feature, not a grammar
dependency.

## The network: many domains, one grammar
The grammar *is* the federation protocol. Because every EMX2 instance resolves the identical
grammar, a record's identity is the tuple **(origin, schema, table, key)** — and its URL is
constructable by any peer without asking the member anything:

```
https://biobank-nl.org/Dataset/row/ds42          (host mode member)
https://emx2.umcg.nl/rare-disease/Patient/row/P7 (path mode member)
```

What this buys the network, concretely:
- **Aggregators need zero per-member config.** A network catalogue that harvests member
  metadata stores the tuple and emits deep links into members mechanically. Today that
  requires knowing each member's app layout; after this, the link format is a constant of
  the network.
- **Citable, stable record IRIs.** `/{schema}/{table}/row/{key}` never changes when a schema
  swaps its front app or when apps are renamed — bare data paths belong to the data, `_`
  paths to the plumbing. That's the property that makes the URL safe to print in a paper.
- **Content negotiation makes the same URL serve humans and machines** (recommended,
  Phase 4): `Accept: text/html` → the front app; `Accept: text/turtle` (etc.) →
  `303 See Other` to `/{schema}/api/rdf/{table}/{key}`. One IRI per record for
  FAIR/linked-data use across the network — and because URL and RDF IRI share the
  `PrimaryKey` encoding, the 303 is a string rewrite, not a lookup.
- **Network-wide search/indexing** falls out of per-schema `sitemap.xml` at stable locations
  plus vanity-domain roots serving `robots.txt`.
- *Deferred, optional:* a machine-readable instance descriptor at `/.well-known/emx2`
  (version, public schemas) so network crawlers can discover members. Only the path
  reservation is decided.

## Registry & validation rules (consolidated)
- Table names: regex already blocks `_`-prefix, dots, hyphens. ADD: reject `api`, `graphql`,
  and the verb set `list|card|graph|aggregate|create|row|edit`. Pin the regex with a test.
- Schema names: reject `api` and `_`-prefix (regex already requires leading letter — pin it).
- App registration: name must not equal a global system route (`login`, `callback`), any
  built-in `_name`, another registered app at the same scope, or any declared asset prefix.
- Apps declare: `scope`, `front` (capability), `assets`, optional `minRole`; per-schema
  **enablement** decides where a custom app is reachable.

## Why this satisfies each goal
- **Short data paths:** the platform's explorer is the default front app and owns bare →
  `/{schema}/{table}/list`.
- **One platform, others too:** front app is configurable per schema; the `_`/`api` boundary
  keeps the platform reachable under any front app — catalogue's nginx whitelist, generalized.
- **Custom apps:** mounted as `_{name}` at the declared scope (or as a front app); never
  collide with tables because `_` is syntactically separate.
- **Vanity domains & the network:** host→schema map; identical grammar at root; uniform
  grammar + structural record keys = constructable cross-member links.
- **Catalogue SSR:** unchanged architecture; simply a front-capable app — the proof of
  concept this generalizes.
- **No forward-compat hazard:** future built-in apps are `_`-prefixed; they cannot break an
  existing user table.

## Migration note
No redirect/back-compat layer for APP urls: the bootstrap→Nuxt rewrite changes those anyway,
so the grammar change rides that rewrite as a clean cutover. Data APIs under
`/{schema}/api/…` are unchanged; the bare `/{schema}/graphql` endpoint is kept as a permanent
alias (see above) so existing API clients keep working.

## Behavior contract
| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Schema = first path segment (path mode) | backend Javalin router (`MolgenisWebservice`) | EXISTS (regression-protect) — TBD link | — |
| `Host` maps to schema; identical grammar at root (host mode) | backend host→schema resolver (NEW) | TBD | — |
| Vanity host exposes ONLY its mapped schema; `/api` there = schema API | host resolver (NEW) | TBD | — |
| Global system routes (`_login`, `_callback`) resolve on vanity hosts before schema apps | resolver (NEW) | TBD | — |
| `robots.txt`/`favicon.ico`/`.well-known/*` served at path-mode root AND vanity roots | resolver / proxy rule (NEW) | TBD | — |
| `/{schema}/api/…` reaches APIs, never an app | backend API routers (unchanged) | EXISTS — TBD link | — |
| `/{schema}/api/graphql` added as canonical; bare `/{schema}/graphql` alias kept | backend GraphqlApi | TBD | — |
| Dotted first segment → well-known file (theme.css, sitemap.xml), never front app | resolver (NEW) | TBD | — |
| `seg` starting `_` → app; bare → front app/table; `api` → API (per depth) | backend resolver + `ServeStaticFile` (NEW) | TBD | — |
| Resolver picks LEFTMOST `_name` at a valid scope boundary; app-internal `_` segments never resolve | resolver (NEW) | TBD | — |
| Table grammar (incl. `_app` mounts) applies under `_explore` same as under bare mount | resolver recursion (NEW) | TBD | — |
| Front app owns bare; platform reachable via `_*`/`api` under any front app | front-app resolver (NEW) | TBD | — |
| Front-capable app dual-mounts (bare when front, `_name` when not) | explorer/catalogue base config (NEW) | TBD | visual check |
| Front app's manifest `assets` prefixes serve its bundle when bare-mounted | resolver + manifest (NEW) | TBD | — |
| `/{schema}/_{app}` mounts a schema-level app, injects schema | app router + context injection (NEW) | TBD | visual check |
| `/{schema}/{table}/_{app}` mounts a table-level app, injects schema+table | app router + context injection (NEW) | TBD | visual check |
| `/{schema}/{table}/row/{key}/_{app}` mounts a record-level app, injects schema+table+row | app router + context injection (NEW) | TBD | visual check |
| `/{schema}/{table}` → 301 `/{table}/list` | explorer router | TBD | — |
| `/{schema}/{table}/list|card|graph|aggregate` render the named view | explorer router | TBD | visual check |
| `/{schema}/{table}/row/{key}` renders a record; key may equal a verb word | explorer router | TBD | — |
| `/{schema}/{table}/row/{key}/edit` opens edit | explorer router | TBD | visual check |
| `/{schema}/{table}/create` opens new-record form | explorer router | TBD | visual check |
| Composite key encodes via `PrimaryKey` (sorted `name=value&…`) in ONE segment; single-key shorthand roundtrips; `=`/`&` inside values survive | explorer router + link builder + core `PrimaryKey` | TBD | — |
| Explorer row URL and RDF row IRI use the SAME key encoding | `PrimaryKey` (promoted to core) | TBD | — |
| `?keys=<JSON>` pattern removed from record URLs | explorer | TBD | — |
| URLs use table identifiers, not display names | explorer link builder | TBD | — |
| Table cannot be named `api`, `graphql`, or a view verb; `_`-prefix/dots impossible by regex (pin) | backend metadata validation | TBD | — |
| Schema cannot be named `api` or start with `_` | backend schema-create validation | TBD | — |
| App declares scope (global/schema/table/record) + front-capability + asset prefixes at registration | app manifest/registration (NEW) | TBD | — |
| Custom app cannot shadow built-in `_name` / system route / asset prefix | registration validation (NEW) | TBD | — |
| Record URL content-negotiates: html → front app, rdf → 303 to `api/rdf` (optional) | backend (NEW) | TBD | — |

## Open / deferred
- Single-key shorthand: emitted always, or only when the value can't be mistaken for a
  `name=value` pair? Decide in Phase 0 with the link-builder.
- Table-level `rowLabel` (display concat of key=1 columns, refs inherit) — endorsed as a
  separate feature; scope/owner TBD, not a grammar blocker.
- Content negotiation scope: RDF only, or also JSON (`Accept: application/json` → api/json)?
- Exact set of view verbs per table type (depends on explorer feature set); freeze before
  Phase 2.
- Host→schema config storage (DB setting vs proxy-only) — see migration Phase 4.
- Per-schema front-app config: reuse `LANDING_PAGE` / first-menu-item, or a new explicit
  setting.
- Manifest filename/format (`molgenis-app.json` vs `package.json` field vs DB row); whether
  `provides` is explicit or the platform is the fallback for unclaimed `_name`s.
- Role-gating granularity: per-app `minRole` vs full per-schema-role app ACLs.
- `/.well-known/emx2` instance descriptor — deferred; only the path reservation is decided.
