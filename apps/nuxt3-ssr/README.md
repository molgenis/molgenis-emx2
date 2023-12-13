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

```bash
npm run dev
```

## Production

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

##### icons

All icons are rendered via the BaseIcon component. 
Additional SVG's can be downloaded from https://fonts.google.com/icons or https://www.flaticon.com/authors/special/lineal ( more illustrative set)
Downloaded svg's should be added to the assets/icons folder.
Icons are transformed using the ```yarn parse-icons``` command ( during development)
The parse-icons command uses the sgvo module to clean the icons and the runs a custom node script to transform the icons to vue components
Icon components are the stored in the /global/icons folder.

##### feature flags

The following feature flag(s) are used to toggle certain app features via the runtime config

- ```cohortOnly```  boolean when set to true the networks part is hidden ( see docker file for passing flag via container)


### debug/test options 

Runtime config options can be set via query param to test/debug options:

- theme: ```theme=[theme-name]```
- logo: ```logo=[logo-file-name-without-extension]```
- feature flag cohorts only: ```cohort-only=true``` // defaults to false

for example ```.../catalogue-demo/ssr-catalogue?cohort-only=true&theme=umcg&logo=UMCGkort.woordbeeld```