# Contributing to MOLGENIS EMX2

MOLGENIS EMX2 is an open source platform built on FAIR data principles (Findability, Accessibility, Interoperability, and Reusability). We are dedicated to building and maintaining EMX2 to support the wider community, and we also welcome contributions from users. Whether you would like to report a bug or add a new feature, contributing to EMX2 can happen in a number of ways. If you would like to contribute to EMX2, use this guide to get started.

## How to contribute

Contributions can be broken down into several categories.

- Have you noticed an issue?
- Do you have an idea for EMX2?
- Have you added to the code base?

### Reporting issues

If you have noticed any issues or something isn't working as expected, we would like to encourage you to first check the following resources.

1. It may be the case that something is missing or needs additional configuration. Please check the [EMX2 documentation](https://molgenis.github.io/molgenis-emx2/#/) for further information.
2. Check the version of EMX2 you have installed. You may need to update or reinstall EMX2
3. If that does not resolve the issue, visit the [issues](https://github.com/molgenis/molgenis-emx2/issues) page. Have a look through the issues to see if the issue hasn't been reported or solved already.

If your issue still isn't resolved, then open a new issue. We have a [Bug report](https://github.com/molgenis/molgenis-emx2/issues/new/choose) template that you can use to report the issue. Provide as much information as you can including screenshots, error messages, and instructions on how to reproduce the issue. Once you have submitted an issue, it will be reviewed by a member of the EMX2 team and the issue will be triaged. We may be in contact about the issue for further information.

### Feature requests

Do you have an idea for EMX2? We welcome any ideas and suggestions for improving EMX2. First, have a look at the [open issues](https://github.com/molgenis/molgenis-emx2/issues) and [open pull requests](https://github.com/molgenis/molgenis-emx2/pulls) to see if the feature doesn't exist. If not, submit a new idea using the [feature request template](https://github.com/molgenis/molgenis-emx2/issues/new/choose). Provide as much detail as you can including links to examples or other resources. A member of the MOLGENIS team will review the request and it will be triaged. Additional information may be requested.

### Code contributions

Anyone is welcome to contribute to the EMX2 code base. To help you get started, we've outlined the process below.

### Pre-development

We have a few guides that provide information on how EMX2 is structured, the coding guidelines, and other practical information. These will help you get started with writing code and to ensure it is consistent with the rest of the code base.

- [Development guidelines](https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_guidelines)
- [Guiding principles and features](https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_principles)
- [MOLGENIS quickstart](https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_quickstart)
- [Technologies we use](https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_technologies)

The easiest way to get started is to start with the latest version of EMX2. Clone the main repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2

# create a new branch
git switch -c feat/<name-of-feature>
```

Regarding branch names, we typically use these patterns for naming branches: `feat/*` for a new feature or improvement, and `fix/` for bugs. Keep the branch names short and concise.

If applicable, open an issue or submit a feature request so that we can link a ticket with a pull request. Please see the previous sections ([Reporting issues](#reporting-issues) and [Feature requests](#feature-requests)) for more information.

### Post-development

We encourage other developers to write unit tests and/or end-to-end tests. For frontend development, we use [Vitetest](https://vitest.dev) and [Playwright](https://playwright.dev). Examples can be found in the `e2e/` and in the `apps/tailwind-components/tests` folders.

When you have finished fixing a bug or developing a new feature, make sure -

- All tests pass and all errors/warnings are fixed
- Add documentation on how to use the new feature or any breaking change. Documentation is located in the `docs/`. If you are unsure where to add the docs, please let us know in the PR.
- Commit all changes and give it a clear and concise message
- Push to the molgenis-emx2 repository and open a new PR. In the PR, provide as much information about the contribution as possible. This includes an overview of your contribution and what was changed/added. Please indicate if there any breaking changes.

New PRs will be reviewed by the MOLGENIS team, triaged, and a reviewer will be assigned. The code review will be scheduled. Additional revisions or changes may be requested.
