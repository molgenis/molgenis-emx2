# Building frontend apps

One of the features of MOLGENIS EMX2, is the ability to create your own frontend applications. This may be useful for creating custom landing pages and dashboards for project monitoring, as well as hosting database documentation within the database. Depending on what you would like to display, you can build it using commonly used frontend tooling and deploy it in MOLGENIS.

This guide will provide information on how to create a new app or how to contribute to an existing application. You will also find information on the tools that are used in MOLGENIS EMX2 and how to configure your app to deploy it in EMX2. In the last section (the appendices), you will find a step-by-step guides for developing apps.

Before you begin, we encourage developers to read our [Contributing Guidelines](https://github.com/molgenis/molgenis-emx2/blog/master/CONTRIBUTING.md) and [Development Guidelines](./dev_guidelines) to ensure consistency of all interfaces. If you get stuck, have a look at the [Troubleshooting guide](./dev_troubleshooting). If you notice any issue with this guide (e.g, broken example, outdated information, etc.), please open an issue on at [molgenis-emx2/issues](https://github.com/molgenis/molgenis-emx2/issues).

## Frontend tools

All frontend apps in EMX2 are developed using the following tools.

- [vuejs](https://vuejs.org/): javascript framework for building web apps
- [Bootstrap 4.x](https://getbootstrap.com/)\*: frontend library for layout and styling (we use last release of v4).
- [vite](https://vitejs.dev): for application bundling
- [yarn workspaces](https://yarnpkg.com/features/workspaces): to autowire local dependencies.

In addition, some of the projects use [Sass](https://sass-lang.com) to compile css. SASS and SCSS can be activated in the vue component files by adding the `lang="scss"` to `<style>` tag.

We also use Gradle to build applications. By running the `build` script in the package.json file, Gradle will build the application and bundle it with the other java applications (during `gradle run`).

## The apps folder

All frontend applications are located in the `apps` folder. In this folder are the vue applications that are used to create the main molgenis interfaces (e.g., schema, settings, tables, etc.) and the applications that we built for specific projects (e.g., aggregates, gportal, etc.). All applications are built using components from one or both of the molgenis component libraries.

- 'molgenis-components': general layout and styling
- 'molgenis-viz': a number of D3 components for creating visualizations and dashboards

These libraries need to be built as it creates a library that can be used in other applications. From time to time, you may need to rebuild the libraries if a library is changed. To build the component libraries, run the following yarn workspace script.

```bash
# if not already in apps/
cd apps

# build both component libraries
yarn build:libs
```

The component libraries are also apps. They create a 'showCase' app that is served as the app code. To view this run:

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

Regarding branch names, please see the "Creating a branch" section in [Basic concepts](./dev_basics) guide.

### Creating a new app

If you would like to create a new vue app. There are few ways to get started. You can either -

1. Copy the `hello-world` demo: a demo application that can be used as a starting for new applications.
2. Copy an existing app and delete any unecessary files
3. Create a new vue app using `yarn create vue@latest`
4. Manually create folder and required files. `mkdir my-app`

The first three options allow you to create apps fairly quickly, but it also requires you to delete files and adjust the configurations. If you would like to create an app manually, follow the [manually creating a frontend application](#manually-creating-a-frontend-application) guide at the end of this page. Before you get started, have a look at the other applications to see how they are structured and configured.

#### Register your application in the yarn workspace

In the apps folder, you will find a `package.json` file. This is where the workspace configurations are defined and all the apps are added to the workspace. Add your application to the list of workspaces so that you have access to all local dependencies.

```json
{
  "private": true,
  "workspaces": [
    // ...
    "my-app"
  ]
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

The `/api` and `/graphql` paths are proxied as defined in the dev-proxy.config.js. In order to preview individual apps, use `yarn dev`. For example, to preview the app `apps/schema`, run the following command.

```bash
cd apps/schema
yarn dev
```

## Deploying your application

When you have finished building your app, commit your changes and open a new PR. See our [contributing guidelines](https://github.com/molgenis/molgenis-emx2/blog/master/CONTRIBUTING.md) for more information on contributing to the EMX2 code base. When your PR is accepted and merged with the main EMX2 branch, a [new release](https://github.com/molgenis/molgenis-emx2/releases) will be created. Then, update your server with the latest version of EMX2.

On your server, all vue apps are served at `/apps/<app-name>`. This mirrors EMX2 folder structure so the URL will match the name of the folder (e.g., `/apps/molgenis-viz`). If your app is designed to work with a schema, it will be accessible at `/<schema>/<app-name>/`.

## Troubleshooting

### How do I view my app locally?

First, start the development server.

```bash
# cd into your app
cd apps/<your-app>

# start the dev server
yarn dev
```

Once started, the app is served at [http://localhost:5173](http://localhost:5173). If the server is running and the app cannot be found, check the `vite.config.js` file to see if the port has changed.

### How do I view my app on the server?

On the server, applications are available at `/apps/<app-name>/`. The path of the app will match the name of the folder in the `apps/` directory. If you app interacts with a schema, it will accessible at `/<schema-name>/<app-name>/`.

If you continue to have issues, make sure your app has been merged with the main emx2 branch and the server is updated with the latest version of MOLGENIS EMX2.

### I get an error that a component from one of molgenis component libraries is not found

It is likely that the component libraries need to built or rebuilt. In the `apps/` folder, run the following command.

```bash
yarn build:libs
```

If that does not resolve the issue, consider deleting the `node_modules` folder, and then reinstalling dependencies and rebuilding the component libraries.

```bash
cd apps/

# remove node_modules
rm -rf node_modules

# reinstall dependencies
yarn

# rebuild component libraries
yarn build:libs
```

### I would like to use the molgenis-viz library, but the styles aren't loading

To use the `molgenis-viz` library in your application, a few configurations are required.

First, make sure the style sheets are defined in the `vite.config.js` file. See if the `css` property is defined and all files are imported. The configuration should like this-

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

If that doesn't work, you may need to rebuild the component libraries. See the troubleshooting issue [I get an error that a component from one of molgenis component libraries is not found](#i-get-an-error-that-a-component-from-one-of-molgenis-component-libraries-is-not-found) for more information.

### I would like to test my app against a clean server

It is possible to run your app against a clean server. We use [gradle](https://gradle.org) to build MOLGENIS locally. You will also need to install and start postgres (see [Postgres](https://www.postgresql.org) for more information).

```bash
./gradlew run
```

Gradle may take some time to build. Once it's ready, the app will be visible on port `8080`

### I want to display my app by default

For projects, you may want to display your app by default so that when a user navigates to a database, your app is displayed by default. Follow these steps to configure this.

1. Sign in to your database as admin
2. On the default view (`/apps/central/`), click on the "Admin" link in the navigation bar.
3. Click on the "Settings" tab
4. Click on "+" to add a new setting.
5. In the form, add the following information:
   - Key: `LANDING_PAGE`
   - Value: `/path/to/your/app`
6. Click save and refresh the page. Click on the MOLGENIS logo to view the change.

The path to your app may vary. If it is tied to schema, then make sure the path is `/<schema-name>/<app-name>/`. Otherwise, the app is available at `/apps/<app-name>/`.

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
  "browserslist": ["> 1%", "last 2 versions"]
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
    proxy: require("../dev-proxy.config")
  }
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
      proxy: require("../dev-proxy.config")
    }
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

By this point, you should have enough to view your app. Run the `yarn dev` command to start the dev server. The app will be served at [http://localhost:5173](http://localhost:5173).

### Generate typescript types for an app

To generate the typescript interfaces for a given schema, run:
`./gradlew generateTypes --args=[schemaName] [full-path+file-name]`

for example on unix: `./gradlew generateTypes --args='catalogue /Users/john/Code/emx2/molgenis-emx2/apps/catalogue/interfaces/generated/types.ts'`
"
or on windows: `.\gradlew generateTypes --args='"catalogue" "C:\Users\john\Code\emx2\molgenis-emx2\apps\catalogue\interfaces\generated\types.ts"' `

The first param is the schema name, second param is the full path to the file the interfaces get generated into.
Note that the file is either created or overridden, and that the folder must already exist.

### Binding a versioned app profile  to a schema

An app may expect a certain schema(model) for the app to function, as profile.

For expample the 'nuxt3-ssr' app expects a schema with certain fields to be present.

A schema can be bound to an app by setting a nonnull value in the profile column of the schema_metadata table.
this is done by adding a profileMigration ( profile and step ) to (data/_profiles/[profile-template.yaml]) template file

for example

profileMigration:
profile: PetStore
step: 3
Any changes to the app code that require a (meta)data change should be accompanied by a migration script that updates the schemas bound to the app.

The version for the profile stored in the org.molgenis.emx2.sql.ProfileMigrations class should be updated to reflect the new version.

Migration steps are java code ( that can call the db) that are added to the org.molgenis.emx2.sql.profilemigrations.[profile-name] package.
Each step should be names as: Step[version].java , where version is the step number ( 0, 1, 2, 3, ...)

For example the 'nuxt3-ssr' app gets extend with a request service, this service expects a 'requests' table to be part of the schema
The version of the app profile is updated by incrementing the version number in the ProfileMigrations class
*A java class containing the migration code is added to the org.molgenis.emx2.sql.profilemigrations.[app-name] package *
On startup the service will check if the schema is bound to an app and up to date with the version set in AppSchemaMigrations
If the app profile is bound but not up to date the migration steps are run until the app profile is up to date