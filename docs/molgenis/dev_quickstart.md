# Quickstart / Pull request review

Below are the steps to checkout the code, optionally a specific branch, build, and then look at the current functionality.

## Clone the code

Clone the latest version of the sourcecode from github using [git](https://git-scm.com/downloads)

```
git clone git@github.com:molgenis/molgenis-emx2.git
```

Optionally, checkout the branch you would like to review:

```
cd molgenis-emx
git checkout <branch name here>
```

Then you can either build + run the whole molgenis.jar, or use docker-compose to instantiate the backend and only run one app, described below. Or you can run
it inside IntelliJ.

## Build whole system

Requires [Postgresql 15](https://www.postgresql.org/download/) and Java 21 (e.g., [OpenJDK 21](https://adoptium.net/)):
Optionally also install python3 for [scripts](use_scripts_jobs.md) feature.

On Linux/Mac this could go as follows (Windows users, please tell us if this works for you too):

**Start postgres using a native postgres installation**

See [Installation guide](run)

**Start postgres using docker-compose**
You can start postgres using `docker-compose`. The data will be mounted in a directory called `psql_data` where you start the docker-compose (default: repo
root-directory)

- Start postgres

  ```console
  cd molgenis-emx2
  docker-compose up -d postgres
  ```

- Dropping the molgenis scheme can be done by deleting the mounted directory on your repo root-directory.
  ```console
  cd molgenis-emx2
  rm -rf psql_data
  ```

**Run a dedicated dev database on a custom port**

To isolate a dev database from the default one on `:5432` without Docker, run a second Postgres **instance** on another port (e.g. `5433`). With Postgres.app, add a second server (PostgreSQL 15) on port `5433`; or start a separate PostgreSQL cluster. A separate *database name* on the same server is **not** enough — see [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree) for why. Seed the `molgenis` db/user if it does not exist yet:

```console
psql -p 5433 -f .circleci/initdb.sql
```

Then point the backend (and tests) at it. The simplest way is a `.env` file at the repo root: Gradle auto-reads it (straight from disk at configuration time, so it is daemon-safe) and forwards every key in it to the forked test/`dev`/`cleandb`/`run` JVMs as a system property, so it also works for settings behind an environment feature flag. Create `.env` with:

```
MOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5433/molgenis
MOLGENIS_POSTGRES_USER=molgenis
MOLGENIS_POSTGRES_PASS=molgenis
```

Now the tasks pick it up with no extra flags:

```console
./gradlew run
./gradlew dev
./gradlew :backend:molgenis-emx2-sql:test
```

A `-D` system property overrides the `.env` value of that key for a single run (handy to hit a different port without editing `.env`):

```console
./gradlew run -DMOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5433/molgenis
```

Add `MOLGENIS_POSTGRES_USER=...`/`MOLGENIS_POSTGRES_PASS=...` (in `.env`) or `-DMOLGENIS_POSTGRES_USER=...`/`-DMOLGENIS_POSTGRES_PASS=...` if your credentials differ from the `molgenis`/`molgenis` default. See `.env-example` for a ready-made template. The `.env` file is gitignored.

**Run the backend on a different HTTP port**

The backend binds `:8080` by default, which is a problem as soon as you keep several checkouts (for example git worktrees, one per branch) running at the same time: the second `./gradlew dev` fails to bind the port, and both would write into the same `molgenis` database. `MOLGENIS_HTTP_PORT` is read the same way as the postgres settings, so one `.env` per checkout describes that checkout's whole dev stack:

```
MOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5435/molgenis
MOLGENIS_POSTGRES_USER=molgenis
MOLGENIS_POSTGRES_PASS=molgenis
MOLGENIS_HTTP_PORT=8083
```

`./gradlew dev` (or `run`) then serves on <http://localhost:8083>, and a `-D` flag still wins for a one-off:

```console
./gradlew dev -DMOLGENIS_HTTP_PORT=8084
```

Stay off `8080` (the default, so somebody else's checkout has it) and off `8081` (historically the webapi test port). Give each parallel checkout its own **Postgres instance** as well, not merely its own database name on the shared server — `gradle test` and `cleandb` drop roles that are cluster-wide.

The app dev servers follow along: `MOLGENIS_APPS_HOST` retargets the ~20 Vite apps and `NUXT_PUBLIC_API_BASE` the three Nuxt apps, both read from the same `.env`. That is the primary workflow, described in full below.

**Start developing using gradle**

- Change into molgenis-emx2 directory and then compile and run via command
  ```
  cd molgenis-emx2
  ./gradlew run
  ```
- View the result on http://localhost:8080

Alternatively you can run inside [IntelliJ IDEA](https://www.jetbrains.com/idea/). Then instead of last ./gradlew step:

- Open IntelliJ and open molgenis-emx2 directory
- IntelliJ will recognise this is a gradle project and will build
- navigate to `backend/molgenis-emx2-run/src/main/java/org/molgenis/emx2`
- Right click on `RunMolgenisEmx2Full` and select 'run'

## Build one 'app'

Usefull for app development without need to rebuild all apps all the time.

Requires postgresql, gradle and [https://npmpkg.com/](https://www.npmjs.com)

- Build the app workspace as a whole (once)
  ```console
  cd molgenis-emx2/apps
  pnpm install
  ```
- Start molgenis 'headless' (i.e. without apps) using gradle (restart on java changes)
  ```console
  cd molgenis-emx2
  ./gradlew dev
  ```
  Verify with <http://localhost:8080/api/graphql>, not with an app URL: `dev` serves **no apps**, so `/apps/central/` returns **404**. That is by design — `molgenis-emx2-webapi` has no dependency on `:apps`. Only `./gradlew run` bundles apps, and it is slow precisely because it pnpm-builds all 27 of them first.
- Serve only the app you want to look at
  ```console
  cd molgenis-emx2/apps/<yourapp>
  pnpm dev
  ```
  Look at the console for the actual port. Apps that declare a `MOLGENIS_PORT_APP_*` key bind that port strictly; the rest fall back to Vite's `5173` or Nuxt's `3000` and drift upwards when it is taken.
- The admin UI is an app too, so in this workflow `central` runs as its own dev server (`cd apps/central && pnpm dev`) rather than under `/apps/central/`.

The next section turns this into the full per-worktree workflow, including how to point those dev servers at a backend that is not on `:8080`.

## Parallel dev stacks (one per worktree)

Everything above assumes a single checkout. If you keep several — for example one git worktree per branch — every checkout defaults to backend `:8080`, dev servers `:3000` and database `molgenis` on Postgres `:5432`, and they quietly fight over all three. Nuxt and Vite make it worse: a busy port is *auto-incremented* rather than refused, so a dev server can end up on a sibling's port and screenshot, query or drop the wrong thing without a single error message.

The convention is: **one worktree declares one complete private stack in one gitignored `.env` at the repo root** — its own Postgres instance, its own backend HTTP port, its own heap cap, and its own dev-server port per app. Declaring a stack costs disk, not memory; nothing starts until you start it. Copy `.env-example` and adjust:

```console
cp .env-example .env
```

| Key | What it moves |
| --- | --- |
| `MOLGENIS_POSTGRES_URI` / `_USER` / `_PASS` | the backend, `cleandb` and every Gradle test task |
| `MOLGENIS_HTTP_PORT` | the port `./gradlew dev` and `run` bind |
| `MOLGENIS_JVM_XMX` | the heap cap of the `dev`/`run` JVM (unbounded when unset) |
| `MOLGENIS_APPS_HOST` | the backend the ~20 shared-proxy Vite apps, `central` and `directory` talk to |
| `NUXT_PUBLIC_API_BASE` | the backend the Nuxt apps (`tailwind-components`, `ui`, `catalogue`) talk to |
| `MOLGENIS_PORT_APP_*` | the port each app's own dev server binds, strictly |
| `MOLGENIS_APPS_SCHEMA` | the schema the schema-less dev-proxy routes fill in |

### Why a separate Postgres instance, and not just a separate database

Postgres roles are **cluster-wide**, and this codebase's role names carry no database name: `MG_ROLE_<schema>/<privilege>` and `MG_USER_<user>`. `cleandb` runs `clean-molgenis-database.sql`, which drops every role in `pg_roles` matching `MG\_%`, `test%` or `user_%`. Test schema names are fixed across checkouts too (`pet store`, `_SYSTEM_`). So `./gradlew cleandb` — or a backend test run — in one checkout destroys roles another checkout's running backend depends on, even when the two use different database names on the same server.

There is a second, nastier failure. A database provisioned by a *different branch* can carry incompatible column types under the same recorded schema version, and the version number stops a migration from repairing it. That really happened here: `MOLGENIS.table_inherits` was `character varying[]` on one instance and `character varying` on another, and every backend test in the second worktree died in `@BeforeAll` with an `ExceptionInInitializerError` that pointed nowhere near the cause. One instance per worktree.

Provisioning an instance is scripted internally, but by hand it is `initdb` into a private data directory, `pg_ctl` on a free port, then `psql -p <port> -f .circleci/initdb.sql`. Registering it as an extra server in Postgres.app makes it visible and disposable from the GUI; leave "start on login" off, or every worktree you ever provisioned spins up a postmaster when you log in.

### Precedence: `.env` beats your shell

Per key, the order is **explicit per-run override → `.env` → ambient environment → code default**, and it is the same on both halves of the stack.

`.env` deliberately wins over the ambient environment, because the alternative cost hours: an `export NUXT_PUBLIC_API_BASE=http://localhost:8080/` in `~/.zshrc` overrode the `.env` of *every* worktree at once, so an app started from a stack declaring `:8083` quietly talked to `:8080` and returned a 502 that read exactly like an application bug. That class of failure is now impossible — and when `.env` does override something you exported, the dev server says so in one line:

```console
WARN  [dev-env] NUXT_PUBLIC_API_BASE=http://localhost:8083/ from .env overrides the ambient http://localhost:8080/ — set MOLGENIS_ENV_OVERRIDE=1 to keep the ambient value
```

The per-run override differs per half:

- **Gradle** — a `-D` system property beats `.env`: `./gradlew dev -DMOLGENIS_HTTP_PORT=9000`. Note that `MOLGENIS_HTTP_PORT=9000 ./gradlew dev` does **not** work: Gradle forwards every `.env` key onto the forked JVM as a system property, and the Java code reads system properties before environment variables.
- **Both halves** — `MOLGENIS_ENV_OVERRIDE=1` restores shell-wins for that one run, and announces each key it keeps:

  ```console
  MOLGENIS_ENV_OVERRIDE=1 MOLGENIS_HTTP_PORT=9000 ./gradlew dev
  MOLGENIS_ENV_OVERRIDE=1 MOLGENIS_PORT_APP_TAILWIND=3999 pnpm dev
  ```

Only keys **present in your `.env`** are affected. `E2E_BASE_URL` is a per-run test knob rather than a stack declaration, so it is never declared in `.env` and still wins in every Playwright config — running e2e against a remote is unchanged. A worktree with **no** `.env` sees no change at all, which is why CI is unaffected; there, and in any un-migrated checkout, Nuxt apps still fall back to their built-in default `https://emx2.dev.molgenis.org/` — the **shared remote**. Give every worktree you still use its own `.env`.

### One `.env`, one backend: the frontend keys must match `MOLGENIS_HTTP_PORT`

`MOLGENIS_APPS_HOST` and `NUXT_PUBLIC_API_BASE` say which backend the dev servers talk to, and `MOLGENIS_HTTP_PORT` says which one this worktree starts. Hand-editing makes it easy to leave them disagreeing — every value individually valid, everything starts, and the frontend quietly serves another worktree's data. So when `.env` points a **local** frontend key at a port other than the declared backend, the dev servers refuse to start:

```console
$ pnpm dev
Error: [dev-env] .env is internally inconsistent: MOLGENIS_HTTP_PORT=8083 is the backend this
stack starts, but MOLGENIS_APPS_HOST=http://localhost:8084 targets local port 8084 — that is
somebody else's backend.
```

Pointing those keys at a **remote** backend (`https://emx2.dev.molgenis.org`) is a normal frontend-only workflow, so nothing is compared there — `MOLGENIS_HTTP_PORT` is simply unused when no local backend is targeted.

### Step 1 — the backend, headless

```console
./gradlew :backend:molgenis-emx2-webapi:dev
```

About nine seconds with a warm daemon and a compiled build, ending in:

```
RunMolgenisEmx2 - with MOLGENIS_HTTP_PORT=8083 (change either via java properties or via ENV variables)
SqlDatabase - with MOLGENIS_POSTGRES_URI = jdbc:postgresql://localhost:5435/molgenis
Javalin - Listening on http://localhost:8083/
```

`dev` is **headless — it serves no apps at all**:

```console
$ curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8083/api/graphql
200
$ curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8083/apps/central/
404
```

That 404 is expected, not a broken build: `molgenis-emx2-webapi` has no dependency on `:apps`, so no app bundle is ever collected. Only `molgenis-emx2-run` depends on `:apps`, which is exactly why `./gradlew run` is slow — it pnpm-builds all 27 apps first. In the normal workflow the admin UI (`central`) runs as its own dev server, step 2.

Stale-`dist` caveat for `run`: apps are collected from `apps/*/dist` by the `collectDist` task, so `run` serves whatever was **last built** into those folders, not necessarily your current source. `rm -rf ./apps/*/dist/` forces a rebuild.

### Step 2 — the apps you are working on

Each app is its own dev server on its own declared port, proxying to the declared backend. Install the workspace once (`cd apps && pnpm install`), then per app:

```console
$ cd apps/tailwind-components && pnpm dev
  ➜ Local:    http://localhost:3031/

$ cd apps/central && pnpm dev
  ➜ Local:    http://localhost:3033/
```

Verify that each really reaches *your* backend and not a sibling's:

```console
$ curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3031/
200
$ curl -s -X POST 'http://localhost:3031/pet%20store/graphql' \
    -H 'Content-Type: application/json' -d '{"query":"{Pet{name}}"}'
{ "data" : { "Pet" : [ { "name" : "pooky" }, ... ] } }

$ curl -s -X POST http://localhost:3033/api/graphql \
    -H 'Content-Type: application/json' -d '{"query":"{_schemas{name}}"}'
{ "data" : { "_schemas" : [ { "name" : "pet store" }, ... ] } }
```

The `tailwind-components` dev log confirms the hop, and the port in it is the one to check:

```
ℹ proxy schema gql request :  /pet store/graphql
ℹ to :  http://localhost:8083/pet store/graphql
```

Three things worth knowing about how that routing works:

- **The Nuxt apps route through a Nitro server route, and only in `dev`.** `tailwind-components` proxies `/<schema>/graphql` in `server/routes/`, using `NUXT_PUBLIC_API_BASE`. Its `build` is `nuxt generate` with `NUXT_PUBLIC_IS_SSR=false`, which has no server routes at all — so the static build does not proxy anything, and `NUXT_PUBLIC_API_BASE` matters only while `nuxt dev` runs. The Nuxt `dev` scripts pass `--dotenv=../../.env`; a missing file there is silently ignored, so a fresh clone still works.
- **The Vite apps share `apps/dev-proxy.config.js`**, which sends `/<schema>/graphql` straight to `MOLGENIS_APPS_HOST`. `central` and `directory` keep their own route maps (central's routes are schema-less: `/api/graphql`, `/theme.css`), also parameterized by `MOLGENIS_APPS_HOST`.
- **`MOLGENIS_APPS_SCHEMA` fills in the schema for the schema-less routes** of those ~20 shared-proxy apps: `/graphql` → `${MOLGENIS_APPS_HOST}/${MOLGENIS_APPS_SCHEMA}/graphql`, and likewise `/reports` and `/theme.css`. It defaults to `pet store` (`directory` defaults to `directory-demo`). If you are developing against any other schema, set it in `.env` — nothing else will point those routes at your data.

A declared port is bound **strictly**: if it is already taken, the dev server errors out with `Unable to find an available port on host "localhost" (tried 3031)` instead of silently walking to `3032` and stealing the next app's port.

### Step 3 — e2e against the running stack

Each app's Playwright config resolves its `baseURL` from that app's own `MOLGENIS_PORT_APP_*`, so the tests hit the server you just started instead of whatever happens to sit on `:3000`. The repo-root `e2e/` suite is the exception: it tests the backend directly and resolves from `MOLGENIS_HTTP_PORT`.

The `tailwind-components`, `ui` and `directory` suites start no server of their own, so they run against the dev server from step 2:

```console
$ cd apps/tailwind-components && pnpm exec playwright test buttonBar --project=chromium --reporter=list
  ✓  1 [chromium] › tests/e2e/buttonBar.spec.ts:12:1 › ... (670ms)
  1 passed (1.2s)
```

`pnpm e2e` runs a whole suite. Only `catalogue` starts its own server, and it starts a **built** one (`node .output/server/index.mjs`) with `reuseExistingServer: false` — so run `pnpm build` first, and stop your catalogue dev server, otherwise the port is taken and the run fails loudly instead of silently testing the wrong thing. That silent-wrong-target failure is the reason for all of this: a stray dev server on `:3000` once made Playwright screenshot a different app entirely and produce blank 45913px baselines that read like a theme bug.

#### Running e2e against a remote — `E2E_BASE_URL`

**Still works, unchanged.** Setting `E2E_BASE_URL` targets that URL regardless of anything the worktree declares, in all five Playwright suites:

```console
E2E_BASE_URL=https://emx2.dev.molgenis.org/ pnpm e2e
```

The full precedence per suite is:

1. `E2E_BASE_URL`, if set — wins over everything;
2. otherwise the app's own `MOLGENIS_PORT_APP_*` (for the repo-root `e2e/` suite, `MOLGENIS_HTTP_PORT`), if declared;
3. otherwise the literal fallback that config always had, so a worktree with no `.env` behaves exactly as before. This is also why CI is unaffected: it exports `E2E_BASE_URL` for the suites it drives that way and declares no ports at all.

Grepping the configs for `process.env.E2E_BASE_URL` now finds nothing — that is **not** a removal. The read moved into the shared `e2eBaseUrl()` resolver in `apps/dev-env.js`, where it is the first branch, and all five configs call it.

**One real behaviour change, worth knowing before it surprises you.** `catalogue`'s fallback `baseURL` is the **remote** `https://emx2.dev.molgenis.org/`, not a localhost URL. So in a worktree that declares `MOLGENIS_PORT_APP_CATALOGUE` and does *not* set `E2E_BASE_URL`, catalogue's e2e now runs against **your local port** where it previously went to the remote. That is the intent — tests should hit the stack you declared — but it is a silent switch in target, so set `E2E_BASE_URL` explicitly whenever you mean the remote.

**A papercut when testing against a remote:** `catalogue`'s config has a `webServer` block, and Playwright starts `webServer` regardless of what `baseURL` points at. So a remote catalogue run still boots a local Nitro server first, waits for it, and then tests the remote. This is pre-existing — it did the same on port 3000 before this change — and the cost is a wasted startup, not a wrong target: your remote run really is hitting the remote. (Read from `apps/catalogue/playwright.config.ts`, not executed.)

### What a stack costs

Measured on macOS with the backend serving GraphQL:

| Process | RSS |
| --- | --- |
| backend JVM at `-Xmx1g` | ~340 MB |
| the Gradle daemon hosting it | ~720 MB |
| one `pnpm dev` Nuxt app (three node processes) | ~885 MB |
| idle postmaster | ~2 MB |
| **one full stack** | **≈ 1.95 GB** |

The Gradle daemon is the one people forget — it costs twice what the backend it hosts costs. `-Xmx` caps heap, not RSS: metaspace, code cache, thread stacks and direct buffers add a few hundred MB on top. Idle declared stacks are nearly free (about 50 MB of disk per data directory and nothing in RAM), so declare freely and start sparingly.

### Running from IntelliJ

**IntelliJ bypasses `.env`.** The forwarding is done by Gradle, and it only reaches JVMs that Gradle forks (`JavaExec` and `Test` tasks). An IDE run configuration launches its own JVM, so it gets none of it — and an unconfigured run therefore targets the **default** database on `:5432` and the default port `:8080`, which is very likely someone else's stack.

For `RunMolgenisEmx2Full`, `RunWebApi` or any other IDE-launched main, set `MOLGENIS_HTTP_PORT`, `MOLGENIS_POSTGRES_URI`, `MOLGENIS_POSTGRES_USER` and `MOLGENIS_POSTGRES_PASS` by hand in the run configuration. Either form works — environment variables, or `-D` VM options — because the resolver checks the system property first and falls back to the environment variable:

```
-DMOLGENIS_HTTP_PORT=8083 -DMOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5435/molgenis
```

The same applies to running tests with the IntelliJ runner instead of the Gradle runner.

## Building on Windows

Since node works very slowly on windows, it is advised to use [WSL](https://learn.microsoft.com/en-us/windows/wsl/install) to speed up your Gradle builds significantly.

When setting up WSL, there are a few things to keep in mind:

- Turn on virtualisation in the BIOS of your computer.
- Install Java binaries within the Linux subsystem.
- Install the correct version of [postgres](https://www.postgresql.org/download/linux/ubuntu/#apt) within the Linux subsystem.
- Make sure to put your git repository on the Linux files system, using a mounted Windows folder is very slow.
- When using VSCode, turn off the `security.restrictUNCAccess` setting
- When using VSCode, install the WSL extension and after this, add all other extensions to the WSL window. This is needed for extensions and intellisense to properly work.

## Tips

last updated 24 nov 2022

### IntelliJ plugins

- We use IntelliJ 2021.3.1 with
  - vue plugin
  - google-java-format plugin
  - prettier plugin, set run for files to include '.vue' and 'on save'
  - auto save and auto format using 'save actions' plugin
  - SonarQube plugin

### Pre-commit hook

We use `gradle format spotlessApply` to ensure code follows standard format. You can use pre-commit build hook in .git/hooks/pre-commit to ensure we don't
push stuff that breaks the build. We have included a gradle task for this if you like. To automatically apply the formatting and update your commit, you can add:

```
./gradlew installPreCommitGitFormatApplyHook
```

### Running tests in intellij

To enable gradle to run tests you must set the test runner to gradle.

In Intellij, go to settings -> Build, Execution, Deployment -> Build tools -> Gradle and then set Run tests using 'IntelliJ' (counter intuitive, don't choose gradle).

See https://linked2ev.github.io/devsub/2019/09/30/Intellij-junit4-gradle-issue/

To skip slow tests that are marked in junit `@Tag('slow')` switch from 'All in package' to 'tags' and set to '!slow' via the 'edit configuration' of your test runner in 'build configuration'

When you get error "java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.util.Map java.util.Collections$UnmodifiableMap.m accessible: module java.base does not "opens java.util" to unnamed module @5cee5251"
that is because you need JVM parameter `--add-opens=java.base/java.util=ALL-UNNAMED`

### Reset cache/daemon

Sometimes it helps to stop the gradle daemon and reset the gradle cache.

```bash
./gradlew --stop && rm -rf $HOME/.gradle/
```

When making changes to `apps/molgenis-components` or similar that other apps depend on without changing those apps themselves,
clearing the cache of all the apps will force gradle to rebuild these again (as these are not stored in the general gradle cache!).

```bash
rm -rf ./apps/*/dist/
```

### Delete all schemas (destroys all your data!)

If you want to delete all the MOLGENIS generated schemas, roles and users in the postgresql and return to clean state, run
`gradle cleandb`

This drops roles across the **whole Postgres instance**, not just one database, so it hits every checkout pointed at that instance. See [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree).

### Build+test drop/creates schemas in my database

Build test ('gradle test') will create database schemas, users, roles and passwords. If you don't like that than please consider to use a different database
instance for 'test'. You can use environment variables MOLGENIS*POSTGRES*\*\* for this. See [Installation guide](run).

### VS code

Some of us also develop using VS code:

- It automatically will discover the gradle tasks
- To enable autoformatting of java using spottless,
  install [spottles plugin](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)

### To enable metrics while running using gradle

MOLGENIS_METRICS_ENABLED=true ./gradlew run

You could for example use prometheus to then view the metrics:

- install prometheus from https://prometheus.io/docs/prometheus/latest/getting_started/
- use the following prometheus.yml config

```
global:
  scrape_interval: 15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.

scrape_configs:
  - job_name: "molgenis"
    static_configs:
      - targets: ["localhost:8080"]
         metrics_path: /api/metrics
        labels:
          app: "molgenis-dev"
```
