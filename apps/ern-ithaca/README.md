# ern-ihaca EMX2 applications

The `ern-ithaca` app is a public facing vue application that displays general information about the project (e.g., about, documents, etc.). There is also a dashboard that displays aggregated data on the participants recruited, status of each center, and other charts.

## Getting Started

### Quick start

Before you start working on the app, it is recommended to create a `.env` file. In it, should be the following variables. This allows you to connect to a remote server.

```zsh
# apps/ern-ithaca/.env
MOLGENIS_APPS_HOST=...
MOLGENIS_APPS_SCHEMA=...
```

For the host, it is possible to use the beta server, but it is strongly recommended to rebuild the database locally and use the localhost instead. ERN servers are updated when there is a necessary update (e.g., ERN-specific feature/fix, security updated, etc.), so they may be several versions behind. By using a local dev environment, we can ensure that the changes can be safely deployed.

The schema should be the name of the schema that is publically accessible.

### Starting the app

After 

## Troubleshooting

### I'm having trouble with routing. When I go to a page, I get an error with queries, page not found, redirected to the same page, etc

This is most likely an issue with the base URL that is defined in the `vite.config.js` file. Change the `base` setting to the following:

```diff
- base: command === "serve" ? "/" : "apps/ern-ithaca/",
+ base: command === "serve" ? "/" : "/",
```

> [!CAUTION]
> This only applies to local development. Do not commit this change.
