# molgenis-viz

The `molgenis-viz` app is a data visualization component library and a showcase of these components. This library is designed for those who would like to create landing pages for their database or develop interactive dashboards. This library includes the following.

* Visualization components with different interactive features (e.g., click events, mouse events)
* Designed to be responsive
* Customizable for any situation (components are minimally styled)

## Get Started

To get started, clone the [molgenis-emx](https://github.com/molgenis/molgenis-emx2) repository.

```sh
git clone https://github.com/molgenis/molgenis-emx2
```

Navigate to the `apps` folder and install the dependencies.

```sh
cd apps
yarn
```

Copy the `hello-world` example app.

```sh
cp hello-world <app-name>
```

Add your new app to the workspace. (All apps are part of the same yarn workspace. Add new apps to the `package.json` file.)

```json
// in package.json
{
  "private": true,
  "workspaces": [
    ...
    "<app-name>",
  ],
  ...
}
```

Navigate to your app and start the development server.

```sh
cd <app-name>
yarn dev
```
