# directory

# Requirements

The application works with the latest Biobank Directory model (or any model that extends this one).
Make sure you create a database using this schema before you use the app.
Molgenis has a builtin template you can use.

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
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
