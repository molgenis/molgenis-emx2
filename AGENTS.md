# MOLGENIS EMX2

Developer docs are in `docs/molgenis/dev_*.md`. Read [dev_guidelines.md](docs/molgenis/dev_guidelines.md) before you write or review code.

## Reviewing pull requests

- For changes to request handling (filters, middleware, authentication, proxies, `apps/*/server/`), first read [How requests reach the backend](docs/molgenis/dev_apps.md#how-requests-reach-the-backend). Then check the change against each caller in that table, including SSR.
- Files under `apps/*/server/` run in production on the SSR path. Do not accept "not used in production" for these files before you trace the SSR caller.
- Keep claims you verified apart from claims you assumed. Mark assumptions as such in review comments, also in tables of callers or clients.
- When a fix changes which requests are accepted or rejected, ask for a regression test in the suite that runs the affected caller. See [Which e2e suites run in CI](docs/molgenis/dev_guidelines.md#which-e2e-suites-run-in-ci).
