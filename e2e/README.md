# MOLGENIS End-to-end testing

For the EMX2 component libraries and applications, we use [playwright](https://playwright.dev) for end-to-end testing. In the `e2e` folder, you will find the tests, documentation, and other configurations for running the molgenis E2E tests. In principle, a single file (`*.spec.ts`) contains one or more tests for one component.

## Geting started

To get started with end-to-end testing in MOLGENIS, clone the molgenis-emx2 repository and navigate to the `e2e` directory.

```bash
git clone https://github.com/molgenis/molgenis-emx2
cd e2e
```

If you are creating a new test or fixing an existing test, create a new branch.

```bash
git switch -c fix/e2e-...
git switch -c feat/e2e-...
```

### Install the dependencies

```bash
yarn install
```

### Running the tests

To test against a localhost:8080 run the following command.

```bash
npx playwright test
```

Additionally, you can also start/stop emx2 when running the tests.

```bash
set CI=true && npx playwright test
```

Rather than changing the working directory to the `e2e` folder, you can also run the tests from the molgenis-emx2 project root.

```bash
npx playwright test --config e2e --project=chromium
```

The test is part of .circleci/config.yml running that same command. When running this on a local emx2 server, run the following first (or add it to `.bash_profile`):

```bash
export E2E_BASE_URL=http://localhost:8080/
```

> [!IMPORTANT]
> Some e2e tests will fail unless the local server is set up correctly!

## playwright vscode plugin

If you are using vscode, we recommend installing the extension: [Playwright: Getting started - VS Code](https://playwright.dev/docs/getting-started-vscode)

Once installed, you can configure the plugin use a local running version of EMX2. In the `settings.json` file, add the following setting.

```json
"playwright.env": {
  "E2E_BASE_URL":"http://localhost:8080/"
}
```

When running tests in files which name start with `admin!`, be sure to run `auth.setup.spec.ts` first to ensure login session is defined. See also: [https://playwright.dev/docs/auth#authenticating-in-ui-mode](https://playwright.dev/docs/auth#authenticating-in-ui-mode).

As the monorepo contains multiple `playwright.config.ts` files, not all test files might appear in the test explorer by default. One can toggle the selected config files in the "Playwright" menu within the "Tesing" tab. Here, click the gear icon to the right of the current active playwright config path to select one or more config files. If selecting multiple config files, the combination of their tests will be shown, though keep in mind tests need their specific config file to be the active one because otherwise they might not function properly.

## Adding tests

We suggest to use the vscode plugin for [creating/recording](https://playwright.dev/docs/codegen) new tests. The `playwright.config.ts` file contains the test configuration including the default server path. It is suggested to use relative server paths ( instead of `https://my-server.com/my-page` use `/my-page` ) to make it possible for test to run against different servers.

By default tests are run for all pull requests, on the server connected to the pull request preview ( i.e. test for pr `007` will  by ( default ) run on `https://preview-emx2-pr-007.dev.molgenis.org/`

If creating tests that require being logged in, ensure the filename starts with `admin!`. For more information about this, see [https://playwright.dev/docs/auth#basic-shared-account-in-all-tests](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests).