# Building frontend 'apps'

In MOLGENIS EMX2, it is possible to create custom frontend applications. This may be useful for creating custom landing pages, dashboards, or other interfaces. This guide will provide information on how to create a new app or contribute to an existing application, as well as detailing the tools that are used and the configurations that are required to build an app. In the last section, you will find a step-by-step guide (see appendices) on how to manually create an application.

If you notice any issue with this guide (e.g, broken example, outdated information, etc.), please open an issue on Github: [molgenis-emx2/issues](https://github.com/molgenis/molgenis-emx2/issues).

## Frontend tools

Frontend apps are developed using the following tools.

- [vuejs](https://vuejs.org/)
- [Bootstrap 4.x](https://getbootstrap.com/)
- [scss](https://sass-lang.com): available via vite and declared in the vue style tags
- [vite](https://vitejs.dev)
- [yarn workspaces](https://yarnpkg.com/features/workspaces) to autowire local dependencies.

We also use Gradle to build applications. By running the `build` script in the package.json file, Gradle will build the application and bundle it with the other java applications (during `gradle run`).

## The apps folder

All frontend applications are located in the `apps` folder. In the apps folder, there are a number of vue applications that are used as the main molgenis frontend (e.g., schema, settings, tables, etc.) or as project specific applications (e.g., aggregates, gportal, etc.). We also have a demo application (`hello-world`) that can be used as a starting for new applications.

All vue applications are built using components from one or both component libraries.

- 'molgenis-components': general layout and styling
- 'molgenis-viz': a number of D3 components built in vue for creating visualizations and dashboards.

When built, a 'lib' is create for each component library that can be used in other applications. You may need to build the libraries the first time and rebuild them after an update.

```bash
# navigate to the apps
cd apps

# build the main component libary
cd molgenis-components
yarn build


# build the visualisation library (if using)
cd ..
cd molgenis-viz
yarn build
```

The component libraries are also apps and they create a 'showCase' that is served as the app code. Note it is also an 'app' so it can be shown inside MOLGENIS. To view this run:

```bash
cd apps
yarn molgenis-components
```

**Note**: The `molgenis-viz` library requires a database as the charts require a dataset to generate. You can use `yarn dev` in the molgenis-viz folder, but you may get an error that the data is missing.

## Getting started

In this section, you will find information on how to create a new app or contribute to an existing one.

### Create a new branch

The easiest way to build your own is app is to start with the latest version of EMX2. Clone the main repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2

# create a new branch
git switch -c feat/my-new-app
```

**Note**: Regarding branch names, we typically use these patterns for naming branches: `feat/*` for a new feature or improvement, and `fix/` for bugs.

### Creating a new app

If you would like to create a new vue app. There are few ways to get started. You can either -

1. Copy the `hello-world` demo
2. Copy an existing app and delete any unecessary files
3. Create a new vue app using `create @yarn---`
4. Manually create folder and required files. `mkdir my-app`

The first three options can create apps fairly quickly, but it requires you to delete files and adjust the configurations. If you would like to create an app manually, follow the [manually creating a frontend application](#manually-creating-a-frontend-application) guide at the end of this page.

#### Register your application in the yarn workspace

In the apps folder, you will find a `package.json` file where all workspaces are defined. Add your application to the list of workspaces so that you have access to all local dependencies.

```json
{
  "private": true,
  "workspaces": [
    // ...
    "my-app",
  ],
  // ...
}
```

### Contributing to an existing app

If you would like to add a feature to an existing app or fix something, then the process is quite simple. Navigate to the application you would to work on and start the development server. Depending on the app, there are two methods for starting the development server.

#### Core MOLGENIS EMX2 applications

In the apps folder, there are several core frontend applications (e.g., settings, table, schema, etc.). These require a molgenis-emx2 backend in order to develop the frontend. You can start the server using docker.

```bash
docker-compose up
```

The /api and /graphql path is then proxied (as defined in the dev-proxy.config.js). In order to preview individual apps using yarn serve. For example, to preview the app `apps/schema`, run the following command.

```bash
cd apps/schema
yarn serve
```

In addition, you can run gradle. In the main folder, run the following command. You will also need to start postgres (see [Postgres]() for more information).

```bash
./gradlew run
```

## Appendices

### Manually creating a frontend application

#### Create a new folder

First, create a new folder in `apps`. Give it a name that is unique and clearly explains the purpose of it. If the app is used in a specific project, then name it after the project.

```bash
cd apps
mkdir *my-app*

cd *my-app*
```

#### Create the package.json file

First, create a `package.json` file in your new app. It is easier to create this using yarn. Follow the prompts and provide as much details as possible.

```bash
yarn init
```

Once the file is created, add the yarn scripts, browserlists, and minimum dependencies. Copy the following code and paste it into the `package.json` file. In the dependencies list, you will need to add the `molgenis-components` library. Rather than specifying a specific version, use `*` to target any local build. If you would like to use the visualization library, add `"molgenis-viz": "*"` to the list of dependencies.

```json
{
  // .... content created by yarn init
  "dependencies": {
    "molgenis-components": "*"
  },
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "serve": "vite preview",
    "format": "prettier src  --write --config ../.prettierrc.js",
    "checkFormat": "prettier src  --check --config ../.prettierrc.js"
  },
  "browserslist": [
    "> 1%",
    "last 2 versions"
  ]
}
```

#### Add dependencies

Our frontend applications do not use that many dependencies. We try to keep the number of dependencies to a minimum. Most of the functionality and tools already exist in the component libraries. If you need anything extra, consider creating a new feature in the component library.

At the very least, you will need the following dependencies to your project.

```bash
yarn add vue vue-router
yarn add -D @vitejs/plugin-vue prettier vite
```

If you would like to interact with the MOLGENIS GraphQL API, install the following dependency:

```bash
yarn add graphql-request
```

#### Add vite.config.js file

Add the `vite.config.js` file to your project

```bash
touch vite.config.js
```

Use the `vite.config.js` file to configure how the application is run and built. At a minimum, the following configurations are needed.

```js
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "",
  server: {
    proxy: require("../dev-proxy.config"),
  },
});
```

By default, all proxy configurations are stored in the `dev-proxy.config.js` file (located in the `apps` folder). This allows you to point the local dev server to an existing EMX2 instance. This may be useful if you would like to query data from a specific database. You can either change the proxy configs in this file or use a `.env` file to store this information.

```sh
# .env file
MOLGENIS_APPS_HOST=....
MOLGENIS_APPS_SCHEMA=....
```

Then, load it into the `vite.config.js` file.

```js
// vite.config.json with .env file
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(() => {
  require("dotenv").config({ path: `./.env` });

  return {
    plugins: [vue()],
    base: "",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  };
});
```

If you are using components for the `molgenis-viz` component library, you will also need import the styles. To use these, add the following configuration after the `server` configuration.

```text
css: {
  preprocessorOptions: {
    scss: {
      additionalData: `
      @import "../molgenis-viz/src/styles/palettes.scss";
      @import "../molgenis-viz/src/styles/variables.scss";
      @import "../molgenis-viz/src/styles/mixins.scss";
      @import "../molgenis-viz/src/styles/resets.scss";
    `,
    },
  },
},
```

#### Add the typescript config file

We are in the process of integrating typescript in our frontend applications. We encourage new applications to following the same approach. This is documented in \[insert link to tailwind guide here].

The easiest way to add typsecript is to copy an existing `tsconfig.json` file from another app. For example, from the `apps/aggregates` folder, create a copy the config file.

```bash
cp ../aggregates/tsconfig.json .
```

We also recommend installing the IDE extensions for more features.

#### Create the project structure

Many projects use a variety of features, so the structure may vary. Projects need to have an `src` (all vue files) and `public` (images and other files) folder.

```bash
mkdir src public
```

For the rest of the project structure, we use the following structure. You may not need everything, but it depends on what your app does. Have a look at the other frontend applications for examples. There are two files that are required to get the app running. These are detailed in the next section.

```text
src/
  components/
  interfaces/
  router/
  styles/
  views/
```

#### Add the base files

Depending on the structure of you project, you may need more folders and files that detailed in the previous section. At a minimun, you will need these files.

```bash
# create base files
touch src/main.ts src/App.vue
```

For the contents of the `main.ts` and `App.vue` file, have a look at the existing projects on how to structure these files as the content will vary by app. At a minimum, the base files should have the following content.

```vue
<!--- App.vue -->
<template>
  <h1>Hello world</h1>
</template>
```

```ts
import { createApp } from "vue";
import App from "./App.vue";

import "molgenis-components/dist/style.css";
import "molgenis-viz/dist/style.css"; // optional
import "./styles/index.scss";

const app = createApp(App);
app.mount("#app");
```

Before you can run the app, you will need the entry point for the app. This is the `index.html` file. It is easier to copy it from an existing project (for example: the aggregates app).

```bash
cp ../aggregates/index.html .
```

Open the index.html file, add update the message with the name of your app. In addition, make sure the script tag points to the `main.ts` file.

Therefore to develop, first cd into to folder 'apps' and install all dependencies for all apps. This will activate yarn
workspaces that also automatically links local dependencies using:

```console
cd apps
yarn install
```
