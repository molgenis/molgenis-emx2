# Nuxt 3 Minimal Starter

Look at the [nuxt 3 documentation](https://v3.nuxtjs.org) to learn more.

## Setup

Make sure to install the dependencies:

```bash
# yarn
yarn install

# npm
npm install

# pnpm
pnpm install --shamefully-hoist
```

## Development Server

Start the development server on http://localhost:3000

set non default (api)proxy target with 
```NUXT_PUBLIC_API_BASE```

```bash
npm run dev
```

## Production

set api-proxy target with 
```NUXT_PUBLIC_API_BASE```

Build the application for production:

```bash
npm run build
```

Locally preview production build:

```bash
npm run preview
```

Checkout the [deployment documentation](https://v3.nuxtjs.org/guide/deploy/presets) for more information.

#### Running the styles

The file `tailwind.config.js` contains all the Molgenis design tokens. The file `main.css` is the base stylesheet and `styles.css` is the stylesheet actually loaded. You can easily flip between one style and another during the develop process. See below for the UMCG example.


##### theme

This theme used the Molgenis theme as its source using the [Tailwind presets](https://tailwindcss.com/docs/presets) option.

A non default emx2 theme is loaded by passing the  ```EMX2_THEME``` environment variable to the startup command.
For example during development

```sh
EMX2_THEME=umcg yarn dev
```

##### feature flags

The following feature flag(s) are used to toggle certain app features via the runtime config

- ```cohortOnly```  boolean when set to true the networks part is hidden ( see docker file for passing flag via container)
- `CATALOGUE_STORE_IS_ENABLED` (boolean): when enabled, the shopping cart will be activated on the collections page (defined in the advanced settings tab)

### debug/test options

Runtime config options can be set via query param to test/debug options:

- theme: ```theme=[theme-name]```
- logo: ```logo=[logo-file-name-without-extension]```
- feature flag cohorts only: ```cohort-only=true``` // defaults to false

for example ```.../catalogue-demo/catalogue?cohort-only=true&theme=umcg&logo=UMCGkort.woordbeeld```

### generate types

gradle generateTypes --args='catalogue apps/catalogue/interfaces/catalogue.ts'