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
  You can verify that it's running by looking at http://localhost:8080
- Serve only the app you want to look at
  ```console
  cd molgenis-emx2/apps/<yourapp>
  pnpm dev
  ```
  Typically the app is then served at http://localhost:9090 (look at the console to see actual port number)

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
