# Troubleshooting

In this guide, you can find answers to some of the frequently asked questions.

## Building frontend apps FAQs

This section contains a number of FAQs to the [Building frontend apps guide](./dev_apps.md).

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

If you continue to have issues, make sure your app has been merged with the main emx2 branch and the server is updated with the [latest version](https://github.com/molgenis/molgenis-emx2/releases/latest) of EMX2.

### I'm getting an error that a component from one of molgenis component libraries is not found. How do I solve this?

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

### How do I use the molgenis-viz library? I imported the components, but the layouts and styles aren't working.

To use the `molgenis-viz` library in your application, a few additional configurations are required.

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

If that doesn't work, you may need to rebuild the component libraries. See the troubleshooting issue [I'm getting an error that a component from one of molgenis component libraries is not found. How do I solve this?](#im-getting-an-error-that-a-component-from-one-of-molgenis-component-libraries-is-not-found-how-do-i-solve-this) for more information.

### I would like to test my app against a clean server

It is possible to run your app against a clean server. We use [gradle](https://gradle.org) to build MOLGENIS locally. You will also need to install and start postgres (see [Postgres](https://www.postgresql.org) for more information).

```bash
./gradlew run
```

Gradle may take some time to build. Once it's ready, the app will be visible on port `8080`

To remove an existing database you can run gradle with the cleandb command.

```bash
./gradlew cleandb
```


### How do I display my app by default when I navigate to my server?

For some projects, you may want to display your app by default so that when a user navigates to a database, your app is displayed by default. Follow these steps to configure this.

1. Sign in to your database as admin
2. On the default view (`/apps/central/`), click on the "Admin" link in the navigation bar.
3. Click on the "Settings" tab
4. Click on "+" to add a new setting.
5. In the form, add the following information:
    - Key: `LANDING_PAGE`
    - Value: `/path/to/your/app` (see below on how to write the path)
6. Click save and refresh the page. Click on the MOLGENIS logo to view the change.

There are two paths that you can use.

1. **Global path**: Apps are always served at `/apps/<app-name>`. This path mirrors the apps folder so the name of the app will be the same as the name of the folder. For example, if you wanted to display the `hello-world` app by default, then set the value of `LANDING_PAGE` to `/apps/hello-world/`.
2. **Schema path**: If the app is designed to work with a specific schema, then write the path using the following format: `/<schema-name>/<app-name>/`. By setting the landing page to an app tied to schema, will also display the navigation menu that is defined in the schema. You may need to adjust the permission levels so that pages are publicly accessible or available after log in.

### How do I test OIDC locally

To test OIDC locally you must have access to a OIDC Identity Provider.
We use keycloak which is setup using the instructions in [Permissions](use_permissions.md).

To test OIDC locally you should first create a client in keycloak for localhost.

Once the client is configured you need to tell your local server to use OIDC.
If you start your server through gradle you can first export the OIDC environment variables.
Fill in the values based on the OIDC client you setup in keycloak.

```bash
export MOLGENIS_OIDC_CLIENT_NAME=
export MOLGENIS_OIDC_CALLBACK_URL=http://localhost:8080
export MOLGENIS_OIDC_CLIENT_ID=
export MOLGENIS_OIDC_CLIENT_SECRET=
export MOLGENIS_OIDC_DISCOVERY_URI=
./gradlew run
```
