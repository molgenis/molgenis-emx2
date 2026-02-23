# HPC Dashboard

Vue-based dashboard for managing HPC jobs, workers, and artifacts in MOLGENIS EMX2. Provides a browser UI for submitting jobs, monitoring execution, uploading artifacts, and removing stale workers.

## Tabs

- **Jobs** — list, filter, submit, and inspect jobs. Job detail shows input/output artifacts and transition history.
- **Workers** — registered HPC head nodes with capabilities and heartbeat status. Stale workers can be removed.
- **Artifacts** — named, typed data objects used as job inputs/outputs. Upload files, commit, and browse.

## Development

```bash
pnpm install
pnpm dev
```

The dev server proxies API calls to a running EMX2 instance (configured in `vite.config.js`). You need to be signed in to access the dashboard.

## Build

```bash
pnpm build
```

Output goes to `dist/`. In production this is served by the EMX2 backend as a plugin app.

## Design

See [doc/design.md](./doc/design.md) for the full protocol specification.
