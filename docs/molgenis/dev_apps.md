# To develop javascript/frontend 'apps'

Frontend apps are developed using [vuejs](https://vuejs.org/) for the logic, and
using [Bootstrap 4.x](https://getbootstrap.com/) for the layout. In addition we
use [yarn workspaces](https://classic.yarnpkg.com/en/docs/workspaces/) to autowire local dependencies. 
Gradle will build all apps (using the 'build' script in package.json) and bundle it with the java apps, e.g. during 'gradle run'.

Therefore to develop, first cd into to folder 'apps' and install all dependencies for all apps. This will activate yarn
workspaces that also automatically links local dependencies using:

```console
cd apps
yarn install
```

There is two shared library called 'molgenis-components' and 'molgenis-viz' that contains all shared components (using bootstrap for styling). 
They produce a 'lib' that is used by the other apps, but also a 'showCase' that is served as the app code. 
Note it is also an 'app' so it can be shown inside MOLGENIS. To view this run:

```console
cd apps
yarn molgenis-components
```

All other folders contain apps created using vite. In order to develop you need to start a molgenis-exm2 backend as
described above, e.g. docker-compose up. The /api and /graphql path is then proxied, see dev-proxy.config.js In order to
preview individual apps using yarn serve. For example, to preview the app 'schema' do:

```console
cd apps/schema
yarn serve
```

To create a new app
* copy from 'hello-world' app as a template
* add to apps/package.json 'workspaces' so it can be used as dependency
* apps/build.gradle will run the 'build' script in package.json automatically scan for internal dependencies in all package.json files to determine build order

To create a new library
* check molgenis-components and molgenis-viz as examples
* add to apps/package.json 'workspaces' so it can be used as dependency
* look at molgenis-components how to include a showCase 
* your package.json should also include a 'build-showcase' if you want to have showCase app
* apps/build.gradle will scan for 'build-showcase' script in your package.json and then add a build step for the showCase app