# Basic concepts

## Development flow

We use:

* [monorepo](https://en.wikipedia.org/wiki/Monorepo), i.e., all code is in [this](http://github.com/molgenis/molgenis-emx2) repository (it is not a monolith).
* [gradle](https://gradle.org/) for build (with yarn 'workspaces' for web app)
    * `gradle build` => builds all
    * `gradle clean` => removes all build artifacts
    * `gradle run` => launches the app including user interface
    * `gradle dev` => launches only the backend (useful when developing frontend using yarn dev)
    * `gradle cleandb` => will empty your postgresql database (MAKE SURE YOU WANT IT)
    * `gradle generateTypes --args='<schema> <file>'` => generates typescript types for schema to file
    * `gradle test` => runs all tests.
    * `gradle testFast` => runs tests excluding those marged using @Tag("slow").

* [github flow](https://guides.github.com/introduction/flow/) which means every pull/merge to master will result in release if indicated in commit message
* [Travis](https://travis-ci.org/molgenis/molgenis-emx2) and [CircleCI](https://travis-ci.org/molgenis/molgenis-emx2) to execute build+test for each commit. See .travis.yml file. Actual release is done on UMCG private Jenkins in light of secure deploy on test servers.
* [Sonar](https://sonarcloud.io/dashboard?id=molgenis_molgenis-emx2) for static quality code checks Major thanks to all these companies!
  
N.B. snapshot docker images can be found at [Docker hub](https://hub.docker.com/repository/docker/molgenis/emx2-snapshot)

### Creating a branch

To get started with EMX2, clone the repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2

git switch -c <type>/<my-branch-name>
```

Make sure the name of the branch is short and concise. Branches must start with one of the following prefixes.

| Prefix   | Description                                                           |
|:---------|:----------------------------------------------------------------------|
| `feat/`  | New features                                                          |
| `fix/`   | bug fixes or minor changes                                            |
| `docs/`  | for anything related to documentation                                 |
| `chore/` | non-production code changes (e.g., updating dependencies, jobs, etc.) |

For example, if you are adding a new component to the library. Name the branch like so:

```bash
git switch -c feat/my-new-component
```

### Semantic Release

We use [Semantic Release](https://github.com/semantic-release/semantic-release) in commit messages. This determines major.minor.patch release.

* `fix(component): message` => results in patch+1 release
* `feat(component): message` => results in minor+1 release
* `BREAKING CHANGE: message` => results in major+1 release
* `build(component): message` => relates to build process, does not result in release.
* `chore(component): message` => relates to other boring stuff.
* Other non-release commands: perf, refactor, test, style, docs.


## Software we use

* postresql
* java
* yarn (on mac, brew install yarn)
* gradle (on mac, brew install gradle)
* git (on mac, install xcode dev tools)
* IntelliJ (kindly provided by JetBrains under open source status)
* CircleCI, Travis and Jenkins (kindly providing open source status)

## Code organisation

```text
[apps]          # contains javascript apps, one folder per app.
[backend]       # contains java modules, one folder per module. 
[data]          # contains data model modules, one folder per module
[docs]          # contains docs, published within EMX2 app and at http://molgenis.github.io/molgenis-emx2
[gradle]        # contains source for gradle
build.gradle    # master build file, typically don't need to edit
settings.gradle # listing of all subprojects for gradle build, edit when adding
docker-compose  # master docker file, useful for quick previews
gradlew         # platform independent build file
```

## Backend modules

* molgenis-emx2: interface and base classes
* molgenis-emx2-sql: implementation into postgresql
* molgenis-emx2-io: emx2 format, csv import/export of data, legacy import
* molgenis-emx2-graphql: all for generating the graphql on top of sql
* molgenis-emx2-semantics: endpoint for linked data serving in json-ld and ttl
* molgenis-emx2-webapi: ties it all together onto SparkJava embedded web server
* molgenis-emx2-datamodels: reusable data models and test data
* molgenis-emx2-run: packages all into one fat jar Work in step
* molgenis-emx2-tasks: toward asynchronous calls for long running transactions/queries
* molgenis-emx2-beacon-v2: beacon services
* molgenis-emx2-rdf: rdf exports
* molgenis-emx2-typescript: generates typescript from schemas
* molgenis-emx2-email: email services
* molgenis-emx2-analytics: services for user analytics
* molgenis-emx2-nonparallel-tests: helper module for tests that cannot be run in parallel
