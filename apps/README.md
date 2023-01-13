# Developer notes

* we use yarn workspaces
* copy 'helloworld' to create a new app
* at first run 'yarn' in 'apps', this will download all dependencies
* then you can simply run 'yarn dev' inside your app folder, e.g. cd to 'helloworld' and run 'yarn dev'

we also have component library in molgenis-components, which can be linked into your app
* when making changes to molgenis-components you have to run 'yarn build' to get your changes available in your app also
