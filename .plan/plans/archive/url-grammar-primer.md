# Primer: Why we're cleaning up EMX2 URL paths

Audience: the team. Goal: agree on *why* we change URL paths, *how* we shrink the app
list, and *how* external/custom apps still fit. Full detail lives in
`.plan/specs/url-grammar.md` (grammar) and `.plan/plans/url-grammar-migration.md` (phases).

---

## TL;DR
Today the same product speaks two opposite URL dialects. We want **one** dialect, built on a
single rule:

> `api` → APIs · `_name` → an app · **bare → your data**

Schema first, shortest possible **data** paths, one **platform** app that owns those paths —
and any other app (catalogue, a custom portal) can "front" a schema the same way, while the
platform stays reachable underneath.

Target shape:
```
/petstore/Pet/list           browse a table        (was /apps/ui/petstore/Pet)
/petstore/Pet/row/123        open a record
/petstore/_settings          settings              (an app: _-prefixed, stable)
/petstore/_my-portal         a custom/external app
/petstore/api/csv/Pet        the data API          (unchanged)
biobanks.org/Dataset/ds42    catalogue fronting a schema (host → schema, vanity domain)
```

---

## 1. Why change anything? Because we already have two contradictory patterns

The backend has always treated **the schema as the first path segment**. Our older
("bootstrap") apps follow that: `/{schema}/{app}`. But the newer `ui` app went the other
way and put the *app framework* first, burying the schema:

| App | URL today | Schema is… |
|-----|-----------|------------|
| tables, settings, schema (bootstrap) | `/petstore/tables#/Pet` | **1st** segment ✅ |
| `ui` (new Nuxt) | `/apps/ui/petstore/Pet/123` | **3rd** segment ❌ |
| catalogue (SSR) | `biobanks.org/Dataset/ds42` (via hand-written nginx) | varies, bespoke |

So the *primary* thing a user cares about — *which dataset am I looking at* — is the first
word in one app and the third in another. That costs us: longer noisy links, no shared prefix
for permissions/bookmarks/analytics, vanity domains only catalogue can do (a one-off nginx
file), and it blocks consolidation — you can't let one app own the short paths if framework
prefixes already occupy them.

**The principle:** decide the schema *once* (from the path, or from the host for vanity
domains), then the *same grammar everywhere*. Schema first. Plumbing never in front of the data.

---

## 2. The one rule that makes everything else fall out

For any segment under a schema:

```
api      → the data API           (always, unchanged)
_name    → an app                 (always reachable, STABLE url, never collides)
bare     → your data              (a table, a row — the front app's content)
```

That's it. No reserved list of app names, no "ask the backend whether this is an app or a
table." The `_` does the work — and it already means "framework, not your data" in EMX2
(`_login`, `_callback`, `_nuxt`). We're extending one existing convention, not inventing one.

Two payoffs worth calling out:

- **A future built-in app can never break an existing table.** If we ship an app called
  `reports` next year and you have a table named `reports`, nothing collides — apps are
  `_reports`, tables are bare. With a reserved-name list that collision is a time bomb; with
  `_` it's structurally impossible.
- **Tables stay clean and short** (`/petstore/Pet/list`) — the `_` cost lands only on app URLs,
  which are the rare case, not on the data browsing you do all day.

---

## 3. How we shrink the app list (consolidation) — the "platform" app

We have 35+ "apps". Most aren't separate products — they're *views of one schema*: tables,
schema designer, settings, pages, tasks, up/download. We fold them into **one platform app**.

That platform app is the schema's **default front app** — it owns the bare namespace, so its
data browser needs no prefix at all:

```
Before:  /petstore/tables#/Pet     /apps/ui/petstore/Pet
After:   /petstore/Pet/list        /petstore/Pet/row/123
```

Its admin surfaces are apps, so they're `_`-prefixed and **stable forever**:

```
/petstore/_settings   /petstore/_schema   /petstore/_pages   /petstore/_tasks
```

These never move, even when a *different* app fronts the schema (next section). Bare is reserved
purely for your data.

---

## 4. "Front apps": how others also get short paths (catalogue, generalized)

There is exactly **one bare (short) namespace per schema**, owned by that schema's **front
app**. Default = the platform. A schema can pick a different front app — and that's how
*others* get short paths:

```
Platform fronts petstore:   /petstore/Pet/list        (platform data browser)
Catalogue fronts a biobank: /biobank/Dataset/ds42     (catalogue, short paths)
```

Crucially, **the platform stays reachable under any front app** — at its stable `_` paths:

```
/biobank/Dataset/ds42     ← catalogue (the front app, bare)
/biobank/_settings        ← platform settings, still here
/biobank/_explore/Sample  ← the data browser, now _-mounted (bare is taken by catalogue)
/biobank/api/csv/Sample   ← APIs, always
```

This is **exactly what catalogue already does** — its nginx config whitelists `/api`, `/apps`
and a few names to the backend and sends everything else to the Nuxt catalogue. We're turning
that hand-maintained whitelist into the one `_`/`api` rule, so it works for *every* schema and
*every* front app with no per-deployment nginx. The only requirement on a front-capable app is
that it can **dual-mount**: run at the bare root when it's the front, and at its `_name` when
it isn't. Catalogue already does this; the platform explorer will too.

---

## 5. How external / custom app code mixes in

Unchanged mechanism, better URLs. Apps get in two ways (both exist today):

- **Bundled apps** ship inside the JAR.
- **External apps** drop into a `custom-app/` folder next to the JAR (`CUSTOM_APP_PATH`).

Under the new grammar a dropped-in app is just `_name` at whatever **scope** it declares:

```
/petstore/_my-portal             schema-level app  (handed the schema)
/petstore/Pet/_my-chart          table-level app   (handed schema + table)
/petstore/Pet/row/9/_audit       record-level app  (handed schema + table + row)
```

An app **declares its scope** (schema / table / record) and whether it's **front-capable**; the
mount depth is exactly the context we inject. Because every app is `_`-prefixed, an external app
slots into the *same* namespace as ours with *zero* collision risk against tables — and a
front-capable external portal can own a schema's short paths just like catalogue. We build all
three scopes up front so the slots are real, not theoretical.

---

## 6. What we need to build

The bootstrap→Nuxt rewrite is changing these app URLs anyway, so the grammar change **rides that
rewrite** — no separate redirect layer, a clean cutover. (Data APIs under `/{schema}/api/…` are
unchanged regardless.)

1. **The `_`/`api`/bare resolver** + name validation (a table can't be named `api` or start with
   `_`). The safety net; do it first.
2. **Platform explorer as default front app at bare root**, with **dual-mount** support (bare
   when front, `_explore` when not). The one genuinely fiddly bit — a built SPA's base is
   build-time; solved with relative links + a unique assets folder + the backend fallthrough.
   Needs a real browser check.
3. **Table grammar** — `/{table}/list|card|graph|create`, `/{table}/row/{id}/edit`.
4. **Table-/record-level app mounting** + context injection.
5. **Front-app mechanism** — per-schema setting for which app owns bare; replace catalogue's
   nginx whitelist with the `_`/`api` rule.
6. **Host → schema mapping** for vanity domains.

---

## What we are NOT changing
- The **data APIs** (`/{schema}/api/csv|excel|rdf|graphql|…`) stay exactly as they are.
- The catalogue stays an **SSR app on its own frontend** — it becomes the *general* front-app
  pattern instead of a bespoke nginx case.
- External apps keep being **drop-in folders** — we're giving them better URLs, not a new SDK.

---

## The one-sentence pitch for the standup
"One rule — `api` → APIs, `_name` → an app, bare → your data — so the platform owns short paths
like `/petstore/Pet/list`, any app (ours, catalogue, or a dropped-in portal) can 'front' a
schema and get those short paths while the platform stays reachable at `_settings`/`_explore`,
and 'my schema on my own domain' becomes a host→schema setting instead of a custom nginx file."
