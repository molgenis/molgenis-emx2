# Components for emx2 project styled using tailwind css

Look at the [Nuxt 3 documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Setup

Make sure to install the dependencies:

```bash

# yarn
yarn install

```

## Development Server

Start the development server on `http://localhost:3000`:

```bash

# yarn
yarn dev

```

## Production

Build the application for production:

```bash

# yarn
yarn build

```

Locally preview production build:

```bash

# yarn
yarn preview

```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.

##### icons

All icons are rendered via the BaseIcon component. 
Additional SVG's can be downloaded from https://fonts.google.com/icons or https://www.flaticon.com/authors/special/lineal ( more illustrative set)
Downloaded svg's should be added to the assets/icons folder.
Icons are transformed using the ```yarn parse-icons``` command ( during development)
The parse-icons command uses the sgvo module to clean the icons and the runs a custom node script to transform the icons to vue components
Icon components are the stored in the /global/icons folder.