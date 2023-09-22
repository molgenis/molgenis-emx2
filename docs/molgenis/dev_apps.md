# To develop javascript/frontend 'apps'

Frontend apps are developed using [vuejs](https://vuejs.org/) and [vue-cli](https://cli.vuejs.org/) for the logic, and
using [Bootstrap 4.x](https://getbootstrap.com/) for the layout. In addition we
use [yarn workspaces](https://classic.yarnpkg.com/en/docs/workspaces/) to autowire local dependencies.

Therefore to develop, first cd into to folder 'apps' and install all dependencies for all apps. This will activate yarn
workspaces that also automatically links local dependencies using:

```console
cd apps
yarn install
```

There is one shared library called 'molgenis-components' that contains all shared components (using bootstrap for styling). Note
it is also an 'app' so it can be shown inside MOLGENIS. To view this run:

```console
cd apps
yarn molgenis-components
```

All other folders contain apps created using vue-cli. In order to develop you need to start a molgenis-exm2 backend as
described above, e.g. docker-compose up. The /api and /graphql path is then proxied, see vue.config.js In order to
preview individual apps using yarn serve. For example, to preview the app 'schema' do:

```console
cd apps/schema
yarn serve
```

To create a new app
* use ```vue create [name]```
* add to apps/package.json 'workspaces' so it can be used as dependency
* copy a vue.config.js from another app to have the proxy.

To create a new library
* use ```vue create [name]```
* add to apps/package.json 'workspaces' so it can be used as dependency
* copy a vue.config.js from another app to have the proxy.
* look at molgenis-components how to include a showCase
* in build.gradle add the package showCase to the buildJavascript task


We use [nx](https://nx.dev/recipes/adopting-nx/adding-to-monorepo) to build all (also when using gradle)
* to build all in 'apps' use command ```yarn nx run-many --target=build,build-showcase```