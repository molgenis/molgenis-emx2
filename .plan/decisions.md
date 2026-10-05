# Decisions (append-only: WHAT changed + WHY + owner + date)

## 2026-06-25 — URL grammar direction (owner: mswertz)
- **Schema-first ordering everywhere.** WHY: schema is the tenant/primary unit; app-first
  nesting (`/apps/ui/{schema}/…`) was judged a regression vs the bootstrap `/{schema}/…`.
  Schema-first also makes vanity domains a proxy concern (drop first segment), not an app rewrite.
- **Flat + reserved app names** (not a reserved `/apps/` wall). WHY: shortest paths; apps and
  tables share the schema root, a segment is an app iff a bundle/registration exists, else a table.
  Trade-off accepted: each app name permanently reserves that table name; enforced by validation.
- **Verb in path for views** (`/{table}/list|card|graph`), records under `/{table}/row/{id}`.
  WHY: owner wants readable verbs; `row/` prefix keeps row ids collision-free against verb words.
  (Considered & rejected: implicit default + `?view=` query param.)
- Captured in `.plan/specs/url-grammar.md`; migration in `.plan/plans/url-grammar-migration.md`.
- **Three app scopes — schema / table / record — defined AND built up front.** WHY: clarifies
  the schema-vs-table-app boundary that the first cut left ambiguous. Scope is declared by the
  app and expressed by mount depth (`/{schema}/{app}` vs `/{schema}/{table}/{app}` vs
  `/{schema}/{table}/row/{id}/{app}`); mount depth == injected context. Reserved sets are
  per-level, so the same name can exist at two scopes without colliding. (Considered: schema-only
  now; and define-but-defer table/record — owner chose build-all-three now.)

## 2026-06-26 — REVERSAL: `_` sigil instead of flat+reserved (owner: mswertz)
- **WHAT changed:** drop "flat + reserved app names"; adopt a single syntactic boundary —
  `api` → APIs, `_name` → an app/platform surface (stable, never collides), **bare → the front
  app's content (user data)**. Reserved set collapses to `api` + the explorer's fixed verbs;
  a table may not be named `api` or start with `_`.
- **WHY:** (1) owner confirmed back-compat is NOT a constraint — the bootstrap→Nuxt rewrite
  breaks these app URLs anyway, so the migration cost that made me steer toward a word-segment
  (`app/`) evaporated. (2) `_` is shortest (owner priority) and already means "framework, not
  data" in EMX2 (`_login`/`_nuxt`). (3) Eliminates the forward-compat hazard: a future built-in
  app can never collide with an existing user table. I had argued AGAINST the sigil on migration
  + SEO grounds; both fell (SEO surface is the bare front app, not `_` app URLs). Reversed.
- **Front-app model:** exactly one bare namespace per schema, owned by the configured **front
  app** (default = platform explorer). Any app can front its schema; the platform stays reachable
  via `_*`/`api` under any front app. This **generalizes catalogue's nginx whitelist** into one
  syntactic proxy rule. Front-capable apps **dual-mount** (bare when front, `_name` when not).
- **Admin always `_`-prefixed:** when the platform is the front app, its admin surfaces still
  live at `/{schema}/_settings`, `/{schema}/_schema`, etc. — stable regardless of front app,
  bare reserved for user data only. (Considered: bare admin paths when platform fronts — rejected
  for reintroducing the reserved list + forward-compat hazard.)
- **No redirect layer** (clean cutover with the rewrite). Spec + plan updated 2026-06-26.

## 2026-07-05 — v2 review adopted: grammar hardening + network framing (owner: mswertz)
- **graphql:** canonical becomes `/{schema}/api/graphql`; bare `/{schema}/graphql` kept as
  PERMANENT alias; `graphql` reserved as table name. WHY: review verified the bare endpoint
  exists today (`GraphqlApi.java:62`) — v1's "APIs all under /api, unchanged" premise was
  false; the front-app fallthrough would have swallowed it. Alias preserves every client.
- **Dotted segments are never tables** (name regex forbids `.`): `theme.css`/`sitemap.xml`
  stay bare as well-known files; `robots.txt`/`favicon.ico`/`.well-known/*` served at all
  roots (incl. vanity hosts).
- **Row identity = `PrimaryKey` encoding** (promoted from rdf module to core): sorted
  `name=value&…` in ONE path segment; bare-value shorthand for single-column keys; replaces
  the ui's `?keys=<JSON>`. Same encoding for explorer URLs and RDF IRIs. WHY: encoder already
  existed + battle-tested; one identity encoding across the network; conneg 303 becomes a
  string rewrite. (Owner asked 2026-07-05 whether this needs a table-level reflabel — answer:
  no, encoding is structural; table-level `rowLabel` endorsed as a SEPARATE display-only
  feature that must never feed URLs.)
- **Resolver rule fixed:** leftmost `_name` at a valid scope boundary (v1 companion doc said
  "deepest", which broke app-internal `_` routes); front-capable app tails re-enter the table
  grammar (record apps work under `_explore` too).
- **Manifests declare asset prefixes** (`assets: ["_nuxt"]`) — bare-mounted front apps would
  otherwise 404 their own build assets.
- **Host mode pinned:** one host = exactly one schema; global system routes win at any root;
  `/api` on a vanity host = that schema's API; cross-schema API only on the canonical host.
- **Network goal made explicit:** uniform grammar = the federation protocol; record identity
  = (origin, schema, table, key); optional content negotiation on row URLs → RDF.
- **Docs:** v2 folded back into `specs/url-grammar.md`; migration plan updated (absorbing the
  app-scoping implementation guide); primer, app-scoping guide, standalone v2, and the
  completed catalogue-split plan archived to `plans/archive/`.
