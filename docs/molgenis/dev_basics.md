# Basic concepts

## Development flow

We use:

* [monorepo](https://en.wikipedia.org/wiki/Monorepo), i.e., all code is
  in [this](http://github.com/molgenis/molgenis-emx2) repository (it is not a monolith).
* [gradle](https://gradle.org/) for build (with yarn 'workspaces' for web app)
    * ```gradle build``` => builds all
    * ```gradle clean``` => removes all build artifacts
    * ```gradle run``` => launches the app
    * ```gradle test``` => runs all tests.
    * ```gradle testFast``` => runs tests excluding those marged using @Tag("slow").

* [Semantic Release](https://github.com/semantic-release/semantic-release) where commit message determines
  major.minor.patch release
    * ```fix(component): message``` => results in patch+1 release
    * ```feat(component): message``` => results in minor+1 release
    * ```BREAKING CHANGE: message``` => results in major+1 release
    * ```build(component): message``` => relates to build process, does not result in release.
    * ```chore(component): message``` => relates to other boring stuff.
    * Other non-release commands: perf, refactor, test, style, docs.
* [github flow](https://guides.github.com/introduction/flow/) which means every pull/merge to master will result in
  release if indicated in commit message
* [Travis](https://travis-ci.org/molgenis/molgenis-emx2) and [CircleCI](https://travis-ci.org/molgenis/molgenis-emx2) to
  execute build+test for each commit. See .travis.yml file. Actual release is done on UMCG private Jenkins in light of
  secure deploy on test servers.
* [Sonar](https://sonarcloud.io/dashboard?id=molgenis_molgenis-emx2) for static quality code checks Major thanks to all
  these companies!

N.B. snapshot docker images can be found
at [Docker hub](https://hub.docker.com/repository/docker/molgenis/emx2-snapshot)

## Software we use

* postresql
* java
* yarn (on mac, brew install yarn)
* gradle (on mac, brew install gradle)
* git (on mac, install xcode dev tools)
* IntelliJ (kindly provided by JetBrains under open source status)
* CircleCI, Travis and Jenkins (kindly providing open source status)

## Code organisation

```
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

* emx2: interface and base classes
* emx2-sql: implementation into postgresql
* emx2-io: emx2 format, csv import/export of data, legacy import
* emx2-graphql: all for generating the graphql on top of sql
* emx2-semantics: endpoint for linked data serving in json-ld and ttl
* emx2-webapi: ties it all together onto SparkJava embedded web server
* emx2-exampledata: test data models and data, used in various test
* emx2-run: packages all into one fat jar Work in step
* emx2-taskList: toward asynchronous calls for long running transactions/queries
