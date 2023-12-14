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

The folder `e2e/catalogue` contains the steps taken by cohort data managers to test the '[new design](https://data-catalogue.molgeniscloud.org/catalogue/ssr-catalogue)' catalogue with multiple user roles in mind. Some of these manual steps will overlap the automated steps described above but we aim to minimize overlap. The tests will be performed on the data catalogue [accept](https://data-catalogue-acc.molgeniscloud.org/catalogue/catalogue/#/networks-catalogue) server which the cohort team updates to a specific version. Only when this version is considered stable the update will be rolled out to the data catalogue production server.

### Catalogue user roles

1. Anonymous user browsing the catalogue.
2. Researcher looking for specific variables.
3. Researcher or data manager looking for information about specific cohorts.
4. Data manager that needs to check the variables their cohort uploaded.
5. Data manager who wants to login in order to upload data (login not yet implemented in this design).
6. Data manager of a network needs to check their network information.

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

The result of these manual tests are saved ['management server'](https://ez3.molgeniscloud.org/catalogue%20test%20results/tables/#/TestResults) emx2. The following attributes are saved.

- Date
- Number
- Version emx2
- Test passed: yes/no
- Comment
- Failed at step
- Known issue: git issue url
- New issue: git issue url
