# Setup playwright and run tests

We have e2e tests using playwright

To install:
```yarn install```

To test against a localhost:8080 run:

```npx playwright test```

You can also make the test start/stop emx2:

```set CI=true && npx playwright test```

You can also run from the molgenis-emx2 project root as follows

```npx playwright test --config e2e --project=chromium```

The test is part of .circleci/config.yml running that same command

## Catalogue manual tests

The folder `e2e/catalogue` contains the steps taken by cohort data managers to test with multiple user roles in mind:

- The '[MOLGENIS catalogue](https://data-catalogue.molgeniscloud.org/catalogue/ssr-catalogue)' .
- The '[UMCG Research Data Catalogue](https://umcgresearchdatacatalogue.nl/UMCG/ssr-catalogue/all/)'
- The '[MOLGENIS development server](https://emx2.dev.molgenis.org/apps/central/#/)'

 Some of these manual steps will overlap the automated steps described above but we aim to minimize overlap. The tests will describe which server and page needs testing. The cohort team manager will specify the [version](https://github.com/molgenis/molgenis-emx2/releases) that will be tested. Only when this version is considered stable the update will be rolled out to the defined production server.

### Catalogue user roles

1. Anonymous user.
2. Researcher.
3. Data manager of a cohort.
4. Data manager of a data source.
5. Data manager of harmonized variables
6. Data manager of a network.

### Catalogue manual test template

The manual test is formatted as markdown and minimally should contain the following information.

- Number
- Role
- Goal
- Steps
  - Step number
  - Action(s)
  - Expected result(s)

### Catalogue manual test results

The cohort data manager will keep track of the performed test by entering their results into [Catalogue tests results](https://data-catalogue.molgeniscloud.org/Catalogue%20tests%20results/tables/#/TestResults).

The following attributes are saved.

- Date
- Number
- Version emx2
- Test passed: yes/no
- Comment
- Failed at step
- Known issue: git issue url
- New issue: git issue url

Add or edit the available tests and emx2 version into the ontology table '[Catalogue tests](https://data-catalogue.molgeniscloud.org/Catalogue%20tests%20results/tables/#/CatalogueTests)' and '[Emx2 version](https://data-catalogue.molgeniscloud.org/Catalogue%20tests%20results/tables/#/Emx2Version)

#### Ontology: Catalogue tests

- name = Test number (1 for example)
- label = Test number - Goal (1 - The data manager of the network ...)

#### Ontology: Emx2 version

- name = Emx2 version (10.35.5)
- parent = Emx2 version family (10.35.0)