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

The file `tailwind.config.js` contains all the Molgenis design tokens. The file `tailwind.css` is the base stylesheet and `styles.css` is the stylesheet actually loaded. You can easily flip between one style and another during the develop process. See below for the UMCG example.

```sh
npx tailwindcss -c ./tailwind.config.js -i ./src/tailwind.css -o ./src/styles.css --watch
```

##### UMCG theme

This theme used the Molgenis theme as its source using the [Tailwind presets](https://tailwindcss.com/docs/presets) option.

```sh
npx tailwindcss -c ./tailwind.config.umcg.js -i ./src/tailwind.css -o ./src/styles.css --watch
```

