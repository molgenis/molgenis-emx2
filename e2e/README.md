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

### Adding tests 

We suggest to use the vscode plugin for creating/recording new (tests https://playwright.dev/docs/codegen)
The `playwright.config.ts` file contains the test configuarion including the default server path. It is suggested to use relative server paths ( instead of `https://my-server.com/my-page` use `/my-page` ) to make it possible for test to run agains diffent servers. 

By default tests are run for all pull requests, on the server connected to the pull request preview ( i.e. test for pr `007` will  by ( default ) run on `https://preview-emx2-pr-3404.dev.molgenis.org/` 
