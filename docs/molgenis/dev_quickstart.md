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

Then pick the path that matches what you are doing:

- **Frontend only** — you work on one or two apps and use a backend somebody else runs. No Postgres, no Java, no backend start:
  jump to [Frontend only: point an app at a backend you did not start](#frontend-only-point-an-app-at-a-backend-you-did-not-start).
- **Fullstack, one checkout** — build + run the whole molgenis.jar, or use docker-compose for the backend and run one app: continue below. Or run it inside IntelliJ.
- **Fullstack, several checkouts at once** (a git worktree per branch): [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree) is the primary workflow, and the rest of this page builds up to it.

Why each rule in that workflow exists — the cluster-wide-roles argument, the memory budget, the proxy routing — is kept out of this page, in [Dev stacks: why it works this way](dev_devstacks.md).

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

**Use a database and an HTTP port that are yours alone**

Both defaults — the `molgenis` database on Postgres `:5432` and backend port `:8080` — are shared with every other checkout on your machine. The second `./gradlew dev` fails to bind `:8080`, and both checkouts write into the same database. One gitignored `.env` at the repo root fixes all of it at once, for Gradle and for the frontend dev servers alike: see [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree).

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

The next two sections turn this into a repeatable workflow, including how to point those dev servers at a backend that is not on `:8080` — at a shared one you did not start, or at your own private stack.

## Frontend only: point an app at a backend you did not start

If you only work on apps, you do not need Postgres, Java or a backend at all. Point the two frontend keys at a backend that already exists — the shared remote, a colleague's server, or a stack running in another checkout — and run the app.

You still need the workspace installed once (`cd apps && pnpm install`). Then create `.env` at the repo root with nothing but a host and the ports of the apps you actually start:

```
MOLGENIS_APPS_HOST=https://emx2.dev.molgenis.org
NUXT_PUBLIC_API_BASE=https://emx2.dev.molgenis.org/
MOLGENIS_PORT_APP_TAILWIND=3031
MOLGENIS_PORT_APP_CATALOGUE=3030
```

That is the whole file. **No `MOLGENIS_HTTP_PORT`, no `MOLGENIS_POSTGRES_*`, no `MOLGENIS_JVM_XMX`** — those four describe a backend *you* start, and you are not starting one. `MOLGENIS_HTTP_PORT` is simply **unused** when the declared host is remote: nothing local is listening for the dev servers to reach.

This is also exactly why the `.env` consistency check compares **loopback targets only**. It refuses to start the dev servers when `MOLGENIS_APPS_HOST` or `NUXT_PUBLIC_API_BASE` names `localhost` on a port other than the declared `MOLGENIS_HTTP_PORT` — but a remote host is never compared against anything, so a remote-pointing `.env` like the one above can never be rejected. Details: [why the check only compares loopback targets](dev_devstacks.md#why-the-consistency-check-only-compares-loopback-targets).

Then start the app you are working on, and check the port in its banner:

```console
$ cd apps/tailwind-components && pnpm dev
  ➜ Local:    http://localhost:3031/
```

Two things that still apply to you:

- If you develop against any schema other than `pet store`, set `MOLGENIS_APPS_SCHEMA` — see [which schema the shared-proxy apps ask for](dev_devstacks.md#which-schema-the-shared-proxy-apps-ask-for).
- e2e against that same remote is `E2E_BASE_URL=https://emx2.dev.molgenis.org/ pnpm e2e`, unchanged.

If you later need your own backend as well, the next section is a superset of this one: same file, more keys.

## Parallel dev stacks (one per worktree)

Everything above assumes there is one backend in play. If you keep several checkouts — for example one git worktree per branch — every checkout defaults to backend `:8080`, dev servers `:3000` and database `molgenis` on Postgres `:5432`, and they quietly fight over all three. Nuxt and Vite make it worse: a busy port is *auto-incremented* rather than refused, so a dev server can end up on a sibling's port and screenshot, query or drop the wrong thing without a single error message.

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
| `MOLGENIS_APPS_SCHEMA` | the schema the schema-less dev-proxy routes fill in ([detail](dev_devstacks.md#which-schema-the-shared-proxy-apps-ask-for)) |

Pick ports nothing else on your machine uses. Stay off `8080` (the default, so somebody else's checkout has it) and off `8081` (historically the webapi test port).

### Step 0 — a Postgres instance for this worktree

Give the worktree its **own Postgres instance** — a second server in Postgres.app, or any separate cluster, on its own port. A separate *database name* on the shared `:5432` server is **not** enough: `gradle test` and `cleandb` drop roles that are cluster-wide, so they reach into every checkout using that server. The full argument, and the incident behind it, is in [why a separate Postgres instance, and not just a separate database](dev_devstacks.md#why-a-separate-postgres-instance-and-not-just-a-separate-database); [provisioning one by hand](dev_devstacks.md#provisioning-an-instance-by-hand) is three commands.

Seed the `molgenis` database and user in the new instance:

```console
psql -p 5435 -f .circleci/initdb.sql
```

Then declare it in `.env`, together with the rest of the stack. The credentials default to `molgenis`/`molgenis`; set `MOLGENIS_POSTGRES_USER` / `MOLGENIS_POSTGRES_PASS` if yours differ. This is the subset used by the walkthrough below — `.env-example` declares a port for **every** app, which is what you want, so that no two checkouts fall back to a shared `5173`:

```
MOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5435/molgenis
MOLGENIS_POSTGRES_USER=molgenis
MOLGENIS_POSTGRES_PASS=molgenis
MOLGENIS_HTTP_PORT=8083
MOLGENIS_JVM_XMX=1g
MOLGENIS_APPS_HOST=http://localhost:8083
NUXT_PUBLIC_API_BASE=http://localhost:8083/
MOLGENIS_PORT_APP_TAILWIND=3031
MOLGENIS_PORT_APP_CENTRAL=3033
```

Every Gradle task in this checkout — `run`, `dev`, `cleandb` and the test tasks — now targets that instance with no extra flags, and so do the dev servers. How that forwarding works: [how the env file reaches each half of the stack](dev_devstacks.md#how-the-env-file-reaches-each-half-of-the-stack). One full stack costs about 1.95 GB of RAM while running ([measured](dev_devstacks.md#what-a-stack-costs-measured)); declared and not started, it costs disk only.

### Precedence: `.env` beats your shell

Per key, the order is **explicit per-run override → `.env` → ambient environment → code default**, and it is the same on both halves of the stack.

`.env` deliberately wins over the ambient environment — [an exported variable in a shell profile used to silently pin every worktree to the wrong backend](dev_devstacks.md#why-the-env-file-beats-your-shell). When `.env` does override something you exported, the dev server says so in one line:

```console
WARN  [dev-env] NUXT_PUBLIC_API_BASE=http://localhost:8083/ from .env overrides the ambient http://localhost:8080/ — set MOLGENIS_ENV_OVERRIDE=1 to keep the ambient value
```

The per-run override differs per half:

- **Gradle** — a `-D` system property beats `.env`: `./gradlew dev -DMOLGENIS_HTTP_PORT=9000`. A plain `MOLGENIS_HTTP_PORT=9000 ./gradlew dev` does **not** work; [why](dev_devstacks.md#why-the-env-file-beats-your-shell).
- **Both halves** — `MOLGENIS_ENV_OVERRIDE=1` restores shell-wins for that one run, and announces each key it keeps:

  ```console
  MOLGENIS_ENV_OVERRIDE=1 MOLGENIS_HTTP_PORT=9000 ./gradlew dev
  MOLGENIS_ENV_OVERRIDE=1 MOLGENIS_PORT_APP_TAILWIND=3999 pnpm dev
  ```

Only keys **present in your `.env`** are affected, so a worktree with no `.env` behaves exactly as it always did. `E2E_BASE_URL` is a per-run test knob, never a declared key, and still wins in every Playwright config.

### One `.env`, one backend: the frontend keys must match `MOLGENIS_HTTP_PORT`

Hand-editing makes it easy to leave `MOLGENIS_APPS_HOST` or `NUXT_PUBLIC_API_BASE` pointing at a different local port than the `MOLGENIS_HTTP_PORT` this worktree starts — every value individually valid, everything starts, and the frontend quietly serves another worktree's data. So when `.env` points a **local** frontend key at a port other than the declared backend, the dev servers refuse to start:

```console
$ pnpm dev
Error: [dev-env] .env is internally inconsistent: MOLGENIS_HTTP_PORT=8083 is the backend this
stack starts, but MOLGENIS_APPS_HOST=http://localhost:8084 targets local port 8084 — that is
somebody else's backend.
```

A **remote** backend is never compared, because [pointing there is a normal frontend-only workflow](#frontend-only-point-an-app-at-a-backend-you-did-not-start).

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

That 404 is expected, not a broken build — no app bundle is ever collected under `dev`, as ["Build one 'app'"](#build-one-app) above explains. In the normal workflow the admin UI (`central`) runs as its own dev server, step 2.

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

The Nuxt apps and the Vite apps get there by different routes, which matters when one of them does not: [how the dev-server proxies route](dev_devstacks.md#how-the-dev-server-proxies-route). If you develop against a schema other than `pet store`, also set `MOLGENIS_APPS_SCHEMA` — [which schema the shared-proxy apps ask for](dev_devstacks.md#which-schema-the-shared-proxy-apps-ask-for).

A declared port is bound **strictly**: if it is already taken, the dev server errors out with `Unable to find an available port on host "localhost" (tried 3031)` instead of silently walking to `3032` and stealing the next app's port.

### Step 3 — e2e against the running stack

Each app's Playwright config resolves its `baseURL` from that app's own `MOLGENIS_PORT_APP_*`, so the tests hit the server you just started instead of whatever happens to sit on `:3000`. The repo-root `e2e/` suite is the exception: it tests the backend directly and resolves from `MOLGENIS_HTTP_PORT`.

The `tailwind-components`, `ui` and `directory` suites start no server of their own, so they run against the dev server from step 2:

```console
$ cd apps/tailwind-components && pnpm exec playwright test buttonBar --project=chromium --reporter=list
  ✓  1 [chromium] › tests/e2e/buttonBar.spec.ts:12:1 › ... (670ms)
  1 passed (1.2s)
```

`pnpm e2e` runs a whole suite. Only `catalogue` starts its own server, and it starts a **built** one (`node .output/server/index.mjs`) with `reuseExistingServer: false` — so run `pnpm build` first, and stop your catalogue dev server, otherwise the port is taken and the run [fails loudly instead of silently testing the wrong thing](dev_devstacks.md#how-each-playwright-suite-picks-its-target).

#### Running e2e against a remote — `E2E_BASE_URL`

**Still works, unchanged.** Setting `E2E_BASE_URL` targets that URL regardless of anything the worktree declares, in all five Playwright suites:

```console
E2E_BASE_URL=https://emx2.dev.molgenis.org/ pnpm e2e
```

Set it explicitly whenever you mean the remote: `catalogue` in particular now runs against your local port when you declare one, where it used to default to the remote. The full per-suite precedence, that behaviour change and one papercut are in [how each Playwright suite picks its target](dev_devstacks.md#how-each-playwright-suite-picks-its-target).

### Running from IntelliJ

**IntelliJ bypasses `.env`**, for both mains and tests: Gradle's forwarding only reaches JVMs Gradle forks, so an unconfigured IDE run targets the default database on `:5432` and port `:8080` — very likely someone else's stack. Set the four keys by hand in the run configuration, as environment variables or as `-D` VM options: [running from IntelliJ](dev_devstacks.md#running-from-intellij-which-bypasses-the-env-file).

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

This drops roles across the **whole Postgres instance**, not just one database, so it hits every checkout pointed at that instance — including the ones using a different database name on it. See [why a separate Postgres instance, and not just a separate database](dev_devstacks.md#why-a-separate-postgres-instance-and-not-just-a-separate-database), and [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree) for the workflow that avoids it.

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
