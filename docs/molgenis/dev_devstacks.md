# Dev stacks: why it works this way

This is the reference behind [Parallel dev stacks](dev_quickstart.md#parallel-dev-stacks-one-per-worktree)
in the [quickstart](dev_quickstart.md). The quickstart tells you what to type on day one; this page
records why each rule exists, which is what you need on the day you are about to change one. Every
number and transcript below was produced by running it.

## Why a separate Postgres instance, and not just a separate database

Postgres roles are **cluster-wide**, and this codebase's role names carry no database name: `MG_ROLE_<schema>/<privilege>` and `MG_USER_<user>`. `cleandb` runs `clean-molgenis-database.sql`, which drops every role in `pg_roles` matching `MG\_%`, `test%` or `user_%`. Test schema names are fixed across checkouts too (`pet store`, `_SYSTEM_`). So `./gradlew cleandb` — or a backend test run — in one checkout destroys roles another checkout's running backend depends on, even when the two use different database names on the same server.

There is a second, nastier failure. A database provisioned by a *different branch* can carry incompatible column types under the same recorded schema version, and the version number stops a migration from repairing it. That really happened here: `MOLGENIS.table_inherits` was `character varying[]` on one instance and `character varying` on another, and every backend test in the second worktree died in `@BeforeAll` with an `ExceptionInInitializerError` that pointed nowhere near the cause. One instance per worktree.

## Provisioning an instance by hand

Provisioning an instance is scripted internally, but by hand it is `initdb` into a private data directory, `pg_ctl` on a free port, then `psql -p <port> -f .circleci/initdb.sql`. With Postgres.app you can instead add a second server (PostgreSQL 15) on the port you picked; any separate PostgreSQL cluster works the same way. Registering it as an extra server in Postgres.app makes it visible and disposable from the GUI; leave "start on login" off, or every worktree you ever provisioned spins up a postmaster when you log in.

## How the env file reaches each half of the stack

Gradle **auto-reads** the repo-root `.env` — straight from disk at configuration time, so it is daemon-safe — and forwards every key in it to the forked test/`dev`/`cleandb`/`run` JVMs as a **system property**. That is why it also works for settings that sit behind an environment feature flag, and why `./gradlew run`, `./gradlew dev` and `./gradlew :backend:molgenis-emx2-sql:test` pick your stack up with no extra flags.

The frontend half reads the same file through `apps/dev-env.js`. The Nuxt `dev` scripts pass `--dotenv=../../.env`; a missing file there is silently ignored, so a fresh clone still works.

## Why the env file beats your shell

`.env` deliberately wins over the ambient environment, because the alternative cost hours: an `export NUXT_PUBLIC_API_BASE=http://localhost:8080/` in `~/.zshrc` overrode the `.env` of *every* worktree at once, so an app started from a stack declaring `:8083` quietly talked to `:8080` and returned a 502 that read exactly like an application bug. That class of failure is now impossible.

A worktree with **no** `.env` sees no change at all, which is why CI is unaffected; there, and in any un-migrated checkout, Nuxt apps still fall back to their built-in default `https://emx2.dev.molgenis.org/` — the **shared remote**. Give every worktree you still use its own `.env`.

`MOLGENIS_HTTP_PORT=9000 ./gradlew dev` does **not** work, for the same reason: Gradle forwards every `.env` key onto the forked JVM as a system property, and the Java code reads system properties before environment variables. Use `-DMOLGENIS_HTTP_PORT=9000`, or `MOLGENIS_ENV_OVERRIDE=1` to restore shell-wins for that one run.

## Why the consistency check only compares loopback targets

`MOLGENIS_APPS_HOST` and `NUXT_PUBLIC_API_BASE` say which backend the dev servers talk to, and `MOLGENIS_HTTP_PORT` says which one this worktree starts. Hand-editing makes it easy to leave them disagreeing — every value individually valid, everything starts, and the frontend quietly serves another worktree's data. That is why `apps/dev-env.js` refuses to load when a **local** frontend key names a port other than the declared backend.

Pointing those keys at a **remote** backend (`https://emx2.dev.molgenis.org`) is a normal [frontend-only workflow](dev_quickstart.md#frontend-only-point-an-app-at-a-backend-you-did-not-start), so nothing is compared there — `MOLGENIS_HTTP_PORT` is simply unused when no local backend is targeted. Failing that legitimate workflow is how a guardrail gets disabled, so the check compares loopback hosts (`localhost`, `127.0.0.1`, `[::1]`) only, and skips silently when either key is absent.

## Running from IntelliJ, which bypasses the env file

**IntelliJ bypasses `.env`.** The forwarding is done by Gradle, and it only reaches JVMs that Gradle forks (`JavaExec` and `Test` tasks). An IDE run configuration launches its own JVM, so it gets none of it — and an unconfigured run therefore targets the **default** database on `:5432` and the default port `:8080`, which is very likely someone else's stack.

For `RunMolgenisEmx2Full`, `RunWebApi` or any other IDE-launched main, set `MOLGENIS_HTTP_PORT`, `MOLGENIS_POSTGRES_URI`, `MOLGENIS_POSTGRES_USER` and `MOLGENIS_POSTGRES_PASS` by hand in the run configuration. Either form works — environment variables, or `-D` VM options — because the resolver checks the system property first and falls back to the environment variable:

```
-DMOLGENIS_HTTP_PORT=8083 -DMOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5435/molgenis
```

The same applies to running tests with the IntelliJ runner instead of the Gradle runner.

## How the dev-server proxies route

- **The Nuxt apps route through a Nitro server route, and only in `dev`.** `tailwind-components` proxies `/<schema>/graphql` in `server/routes/`, using `NUXT_PUBLIC_API_BASE`. Its `build` is `nuxt generate` with `NUXT_PUBLIC_IS_SSR=false`, which has no server routes at all — so the static build does not proxy anything, and `NUXT_PUBLIC_API_BASE` matters only while `nuxt dev` runs.
- **The Vite apps share `apps/dev-proxy.config.js`**, which sends `/<schema>/graphql` straight to `MOLGENIS_APPS_HOST`. `central` and `directory` keep their own route maps (central's routes are schema-less: `/api/graphql`, `/theme.css`), also parameterized by `MOLGENIS_APPS_HOST`.

## Which schema the shared-proxy apps ask for

**`MOLGENIS_APPS_SCHEMA` fills in the schema for the schema-less routes** of those ~20 shared-proxy Vite apps: `/graphql` → `${MOLGENIS_APPS_HOST}/${MOLGENIS_APPS_SCHEMA}/graphql`, and likewise `/reports` and `/theme.css`. It defaults to `pet store` (`directory` defaults to `directory-demo`). If you are developing against any other schema, set it in `.env` — nothing else will point those routes at your data. Routes that already carry a schema in the path are unaffected.

## How each Playwright suite picks its target

Silently testing the wrong target is the reason for all of this: a stray dev server on `:3000` once made Playwright screenshot a different app entirely and produce blank 45913px baselines that read like a theme bug. Hence strictly bound ports, `reuseExistingServer: false`, and a target resolved from the stack you declared.

The full precedence per suite is:

1. `E2E_BASE_URL`, if set — wins over everything;
2. otherwise the app's own `MOLGENIS_PORT_APP_*` (for the repo-root `e2e/` suite, `MOLGENIS_HTTP_PORT`), if declared;
3. otherwise the literal fallback that config always had, so a worktree with no `.env` behaves exactly as before. This is also why CI is unaffected: it exports `E2E_BASE_URL` for the suites it drives that way and declares no ports at all.

`E2E_BASE_URL` is a per-run test knob rather than a stack declaration, so it is never declared in `.env` and still wins in every Playwright config — running e2e against a remote is unchanged.

Grepping the configs for `process.env.E2E_BASE_URL` now finds nothing — that is **not** a removal. The read moved into the shared `e2eBaseUrl()` resolver in `apps/dev-env.js`, where it is the first branch, and all five configs call it.

**One real behaviour change, worth knowing before it surprises you.** `catalogue`'s fallback `baseURL` is the **remote** `https://emx2.dev.molgenis.org/`, not a localhost URL. So in a worktree that declares `MOLGENIS_PORT_APP_CATALOGUE` and does *not* set `E2E_BASE_URL`, catalogue's e2e now runs against **your local port** where it previously went to the remote. That is the intent — tests should hit the stack you declared — but it is a silent switch in target, so set `E2E_BASE_URL` explicitly whenever you mean the remote.

**A papercut when testing against a remote:** `catalogue`'s config has a `webServer` block, and Playwright starts `webServer` regardless of what `baseURL` points at. So a remote catalogue run still boots a local Nitro server first, waits for it, and then tests the remote. This is pre-existing — it did the same on port 3000 before this change — and the cost is a wasted startup, not a wrong target: your remote run really is hitting the remote. (Read from `apps/catalogue/playwright.config.ts`, not executed.)

## What a stack costs, measured

Measured on macOS with the backend serving GraphQL:

| Process | RSS |
| --- | --- |
| backend JVM at `-Xmx1g` | ~340 MB |
| the Gradle daemon hosting it | ~720 MB |
| one `pnpm dev` Nuxt app (three node processes) | ~885 MB |
| idle postmaster | ~2 MB |
| **one full stack** | **≈ 1.95 GB** |

The Gradle daemon is the one people forget — it costs twice what the backend it hosts costs. `-Xmx` caps heap, not RSS: metaspace, code cache, thread stacks and direct buffers add a few hundred MB on top. Idle declared stacks are nearly free (about 50 MB of disk per data directory and nothing in RAM), so declare freely and start sparingly.
