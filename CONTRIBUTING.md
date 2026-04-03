# Contributing to MOLGENIS EMX2

MOLGENIS EMX2 is an open source platform built on FAIR data principles (Findability, Accessibility, Interoperability, and Reusability). We are dedicated to building and maintaining EMX2 to support the wider scientific community, and we welcome contributions from anyone. Whether you would like to report a bug or add a new feature, contributing to EMX2 can happen in a number of ways. Use this guide to get started.

## How to contribute

The review of contributions will take place on GitHub [molgenis-emx2 repository](https://github.com/molgenis/molgenis-emx2). A GitHub account is required to contribute. Depending on the type of contributions (issues, features, code), there are different methods for contributing to the MOLGENIS EMX2 software. These are described in the following sections.

### Reporting issues

If you have noticed any issues or something isn't working as expected, first check the following resources.

1. It may be the case that something is missing or needs additional configuration. Please check the [EMX2 documentation](https://molgenis.github.io/molgenis-emx2/#/) for further information.
2. Check the version of EMX2 you have installed. You may need to update or reinstall EMX2
3. If that does not resolve the issue, visit the [issues](https://github.com/molgenis/molgenis-emx2/issues) page. Have a look through the issues to see if the issue hasn't been reported or if it was already solved.

If the issue remains unsolved, then open a new issue. We have a [Bug report temport](https://github.com/molgenis/molgenis-emx2/issues/new/choose) that can be used to report issues. Provide as much information as you can including screenshots, error messages, and instructions on how to reproduce the issue. It is also important to provide information about the browser you are using and the MOLGENIS version number that you have installed.

### Feature requests

We welcome any ideas and suggestions for improving EMX2. First, have a look at the [open issues](https://github.com/molgenis/molgenis-emx2/issues) and [open pull requests](https://github.com/molgenis/molgenis-emx2/pulls) to see if there isn't a feature request or something similar. Otherwise, you can submit a request using the [feature request template](https://github.com/molgenis/molgenis-emx2/issues/new/choose). Provide as much detail as you can including links to examples or other resources.

### Code contributions

Anyone is welcome to contribute to the EMX2 code base. We've outlined this process below.

#### Pre-development

We have created several [developer guides](https://molgenis.github.io/molgenis-emx2/#/dev) that provide information on how EMX2 is structured, the coding guidelines, and other practical information. These will help you get started with writing code and to ensure it is consistent with the rest of the code base.

The easiest way to get started is to start with the latest version of EMX2. Clone the main repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2

# create a new branch
git switch -c feat/<name-of-feature>
```

Regarding branch names, please see the "Creating a branch" section in [Basic concepts](https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_basics) guide.

If applicable, open an issue or submit a feature request so that we can link a ticket with a pull request. Please see the previous sections ([Reporting issues](#reporting-issues) and [Feature requests](#feature-requests)) for more information.

#### Post-development

We encourage other developers to write unit tests and/or end-to-end tests. For frontend development, we use [Vitetest](https://vitest.dev) and [Playwright](https://playwright.dev). Examples can be found in the `e2e/` and in the `apps/tailwind-components/tests` folders.

When you have finished fixing a bug or developing a new feature, make sure -

- All tests pass and all errors/warnings are fixed
- Add documentation on how to use the new feature or any breaking change. Documentation is located in the `docs/`. If you are unsure where to add the docs, please let us know in the PR.
- Commit all changes and give it a clear and concise message
- Push to the molgenis-emx2 repository and open a new PR. In the PR, provide as much information about the contribution as possible. This includes an overview of your contribution and what was changed/added. Please indicate if there any breaking changes.

## The submission process

All contributions (bug report, feature request, pull request) will be reviewed by a member of the MOLGENIS-EMX2 team. Then, the following steps will take place.

1. **Triage of the contribution**: We will first triage the request/bug report/PR. It will be reviewed to make sure it is clear. We may request additional information.
2. **Assignment and scheduling**: Depending on the urgency and priority of the issue/feature/etc, we will schedule items accordingly. We schedule tickets into 3-week sprints. Please keep in mind that it may take a little while until the request is processed. If you have submitted a new issue or feature request, we will link all code commits to it.
3. **Review of the contribution**: For code contributions, we will review the code and perform a function review. We may request additional changes.

When everything is thoroughly reviewed and accepted, we will merge it with the main EMX2 branch. Our continuous integration (CI) jobs will build the software and create a new release of EMX2 at [molgenis-emx2/releases](https://github.com/molgenis/molgenis-emx2/releases).
