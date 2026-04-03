# Catalogue manual tests

The folder `e2e/catalogue` contains the manual test used by cohort data managers to test with multiple user roles in mind. For example the markdown file `catalogue-test_1.md`.

 Some of these manual steps will overlap the automated steps described above but we aim to minimize overlap. The tests will describe which server and page needs testing. The cohort team manager will specify the [version](https://github.com/molgenis/molgenis-emx2/releases) that will be tested. Only when this version is considered stable the update will be rolled out to the defined production server.

## Catalogue user roles

1. Naïve user.
2. Researcher.
3. Data manager of a cohort.
4. Data manager of a data source.
5. Data manager of harmonised variables
6. Data manager of a network.

## Catalogue manual test template

The manual test is formatted as markdown and minimally should contain the following information.

- Number
- Role
- Goal
- Steps
  - Step number
  - Action(s)
  - Expected result(s)
  - Github bug/issue (If step has known bug or issue set to: bug/issue number)
  - Playwright test (If step is covered by playwright set to: true)

## Catalogue playwright test

The folder `e2e/catalogue` also contains the playwright tests. These follow the same naming schema but their extension end with `spec.ts`. For example the manual test `catalogue-test_1.md` is saved in the folder `e2e/tests/catalogue/` as playwright test: `catalogue-test_1.spec.ts`

### playwright vscode plugin

[Playwright: Getting started - VS Code](https://playwright.dev/docs/getting-started-vscode)