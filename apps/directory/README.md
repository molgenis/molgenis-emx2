# directory

# Requirements

The application works with the latest Biobank Directory model (or any model that extends this one).
Make sure you create a database using this schema before you use the app.
Molgenis has a builtin template you can use.

# Negotiator settings

The application offers support for two versions of the negotiator requests.
For the most recent (default) version you can set to following settings:

```
"negotiatorType": "v3",
"negotiatorUrl": "https://url.to.negotiator.org",
```

For using the legacy Podium api, which requires authentication, use:

```
"negotiatorType": "v1",
"negotiatorUrl": "https://url.to.podium.negotiator.org",
"negotiatorUsername": "user name",
"negotiatorPassword": "password",
```

# Using Matomo to track user behaviour

If you want to use Matomo to track how users use the app, you can in the settings add the following:

```
"matomoUrl": "https://your.matomo.host",
"matomoSiteId": "yourSiteId"
```

# Login path for access to graphql query

When you do

```sh
yarn dev
```

open http://127.0.0.1:5173/apps/central/#/ and login

see vite.config.js for details about the current server

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

## Project Setup

```sh
yarn
```

### Compile and Hot-Reload for Development

```sh
yarn dev
```

### Compile and Minify for Production

```sh
yarn build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
yarn test:unit
```

### Lint with [ESLint](https://eslint.org/)

```sh
yarn lint
```
