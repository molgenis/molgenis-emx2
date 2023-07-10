#

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