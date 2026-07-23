# Quickstart / Pull request review

Below are the steps to checkout the code, optionally a specific branch, build, and then look at the current functionality.

## Clone the code

Clone the latest version of the sourcecode from github using [git](https://git-scm.com/downloads)

```
git clone git@github.com:molgenis/molgenis-emx2.git
```

Optionally, checkout the branch you would like to review:

```
cd molgenis-emx2
git checkout <branch name here>
```

Then you can either build + run the whole molgenis.jar, or use docker-compose to instantiate the backend and only run one app, described below. Or you can run
it inside IntelliJ. Work on apps only? See [Frontend only: point an app at a backend you did not start](#frontend-only-point-an-app-at-a-backend-you-did-not-start). Keep more than one checkout? Read [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree) first.

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
  Verify with `/api/graphql` on whatever port you declared (<http://localhost:8080/api/graphql> if you have no `.env`), not with an app URL: `dev` serves **no apps**, so `/apps/central/` returns **404**. Only `./gradlew run` bundles apps. The admin UI is an app too, so here you run it yourself with `cd apps/central && pnpm dev`.
- Serve only the app you want to look at
  ```console
  cd molgenis-emx2/apps/<yourapp>
  pnpm dev
  ```
  Look at the dev server's own banner for the actual port: Vite apps start at `5173` and Nuxt apps at `3000`, and drift upwards when that port is taken.

## Frontend only: point an app at a backend you did not start

If you only work on apps, you need no Postgres, no Java and no backend of your own. Install the workspace once (`cd apps && pnpm install`), then create a gitignored `.env` at the repo root with nothing but the host of a backend that already exists:

```
MOLGENIS_APPS_HOST=https://emx2.dev.molgenis.org
NUXT_PUBLIC_API_BASE=https://emx2.dev.molgenis.org/
```

That is the whole file — no `MOLGENIS_HTTP_PORT` and no `MOLGENIS_POSTGRES_*`, since those describe a backend you start yourself. Then `cd apps/<yourapp> && pnpm dev`. Set `MOLGENIS_APPS_SCHEMA` if you develop against a schema other than `pet store`.

## Parallel dev stacks (one per worktree)

Every checkout defaults to backend port `:8080` and the `molgenis` database on Postgres `:5432`, so several checkouts — for example a git worktree per branch — quietly fight over both. The convention is: **one worktree declares one private stack in one gitignored `.env` at the repo root.** Copy `.env-example` and adjust.

Give the worktree its **own Postgres instance** — a second server in Postgres.app, or any separate cluster, on its own port. A separate *database name* on the shared `:5432` server is **not** enough: `gradle test` and `cleandb` drop roles that are cluster-wide, so they reach into every checkout using that server. Seed the new instance:

```console
psql -p 5435 -f .circleci/initdb.sql
```

Then declare the stack, picking ports nothing else on your machine uses — stay off `8080` (the default, so somebody else's checkout has it) and off `8081` (historically the webapi test port):

```
MOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5435/molgenis
MOLGENIS_POSTGRES_USER=molgenis
MOLGENIS_POSTGRES_PASS=molgenis
MOLGENIS_HTTP_PORT=8083
```

Every Gradle task in this checkout — `run`, `dev`, `cleandb` and the test tasks — now targets that instance with no extra flags, and every app dev server proxies to `http://localhost:8083`. Start the stack as above: `./gradlew dev` plus a `pnpm dev` per app.

### Which backend a frontend app (`pnpm dev`) talks to

Vite apps read `MOLGENIS_APPS_HOST`, Nuxt apps read `NUXT_PUBLIC_API_BASE`. Both
resolve the same way — the first match wins:

1. the key itself, set in `.env`
2. `http://localhost:<MOLGENIS_HTTP_PORT>`, derived from the port in `.env`
3. the key exported in your shell environment
4. the app's own fallback (usually `https://emx2.dev.molgenis.org`)

Note that `.env` beats your shell environment. This is on purpose, and it is the opposite of the usual dotenv rule, because an `export NUXT_PUBLIC_API_BASE=...` in a shell profile would otherwise point *every* worktree at one backend that none of them declared. Note that this order applies to **every** key in `.env`.

For a one-off change, use a `-D` system property on the Gradle command line — it
beats `.env`:

`./gradlew dev -DMOLGENIS_HTTP_PORT=9000`

### Running from IntelliJ

**IntelliJ does not read `.env`.** Gradle's forwarding only reaches JVMs Gradle forks, so an unconfigured IDE run targets the default database on `:5432` and port `:8080` — very likely someone else's stack. Set `MOLGENIS_HTTP_PORT`, `MOLGENIS_POSTGRES_URI`, `MOLGENIS_POSTGRES_USER` and `MOLGENIS_POSTGRES_PASS` by hand in the run configuration, as environment variables or as `-D` VM options.

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

This drops roles across the **whole Postgres instance**, not just one database, so it hits every checkout pointed at that instance — including the ones using a different database name on it. See [Parallel dev stacks](#parallel-dev-stacks-one-per-worktree).

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
