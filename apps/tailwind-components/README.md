# MOLGENIS-EMX2 Component library

The `tailwind-components` library is a reimplementation of the existing emx2 components using nuxt and tailwind. This allows us to style build interfaces using a standard set of components to ensure all interfaces have a consistent layout and design.

## About the components

All components are stored in the `components` folder and stories are located in `pages`. To view the components, take a look at the [components showcase](https://emx2.dev.molgenis.org/apps/tailwind-components) or run the `yarn dev` command.

In the component folder, we've arranged the components into categories. To use the components, you will need to prefix the component name using the name of the subfolder. For example, if you wanted to use the checkbox component (`components/input/Checkbox.vue`), you would write the component as `<InputCheckbox>`. There are some folders that do not require the path prefix. This is summarised in the following table.

| Folder  | Prefix       | Description                                                                    | Example                |
|:--------|:-------------|:-------------------------------------------------------------------------------|------------------------|
| display | `<Display*>` | these components can be used to specify the presentation of content            | `<DisplayList>`        |
| filter  | `<Filter*>`  | these components are used to reduce content by user specified criteria         | `<FilterSearch>`       |
| global  | ---          | these components are globally available (path prefix is not needed)            | `<ArrowLeft>`          |
| input   | `<Input*>`   | these components are primarily used in forms                                   | `<InputCheckbox>`      |
| viz     | ---          | a suite of visualization components built using D3 (path prefix is not needed) | `<GroupedColumnChart>` |

### Icons

There are a number of icons available for use. See `/components/global/icons` for a complete list or visit the [Icons Showcase](https://emx2.dev.molgenis.org/apps/tailwind-components/#/Icons.story). All icons are sourced from [https://fonts.google.com/icons](https://fonts.google.com/icons) and [https://www.flaticon.com/authors/special/lineal](https://www.flaticon.com/authors/special/lineal).

### Using icons

These are located in the `components/global/icons` folder. For convenience, use the `<BaseIcon>` component to render icons. This applies some default styling so that you don't have to.

```html
<BaseIcon name="ArrowUp" />
```

Alternatively, you can import the icons directly. Please note that the default icon width is `24`. Make sure this is added to the component.

```html
<ArrowUp width="24" />
```

**Note**: all icons are available globally. You do not need to prefix the icons with `Icons*` or `GlobalIcons*`. Simply type the name of the icon (e.g., `ArrowUp`).

### Adding new icons

Before adding a new icon to the component library, make sure there isn't a similar icon in `/global/icons` folder. If an appropriate icon isn't available, download the svg from [https://fonts.google.com/icons](https://fonts.google.com/icons) and [https://www.flaticon.com/authors/special/lineal](https://www.flaticon.com/authors/special/lineal). Place the file in the `assets/icons` folder and run the parse icons command.

```bash
yarn parse-icons
```

This script uses the [sgvo](https://github.com/svg/svgo) module to clean the icons and transform them into vue components. These files are then saved in the `global/icons/` folder.

## Getting started

To work on the component library locally, clone the molgenis-emx2 repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2
git switch -c <my-branch>
```

### Setup

We use [yarn workspaces](https://yarnpkg.com/features/workspaces) to manage dependencies across all applications. On the first time, install the dependencies.

```bash
yarn
```

If you would like to add one or more libraries, install them using the `yarn add` method.

```bash
yarn add <package-name> # add the -D flag if needed
```

### Development Server

To start the development server, run the following command. By default, the application will be served at `http://localhost:3000`.

```bash
yarn dev
```

### Production

You can also build the application for production and preview it locally before creating a PR. To do so, run the following commands.

```bash
yarn build
yarn preview
```

### Running tests

#### Accessibility testing

[pa11y-ci](https://github.com/pa11y/pa11y-ci) is used for local accessibility testing. We have configured the tests to evaluate components individually against the WCAG2AA standard. To run the tests, follow these steps.

1. Start the development server for the tailwind components: `yarn dev`
2. In a separate terminal window, run the accessibility tests: `yarn a11y:test`
3. When the tests are complete, view the report: `yarn a11y:serve` (the report will be served at `http://localhost:1234`)

It is recommended to address all errors and warnings identified by this tool. In addition, it is also recommended to install the [WAVE Browser extension](https://wave.webaim.org/extension/) for in-browser accessibility testing as this can help identify issues earlier in the development process. If errors unrelated to the component are detected, please note this in a new issue.

### Creating a new PR

We warmly welcome PRs to the molgenis-emx2 repository. To get started, please see our Contributing Guidelines (tbd). Before you create a new PR, please complete the following steps.

1. Make sure all typescript issues are resolved
2. If applicable, write tests and make sure all tests pass. In the `tailwind-components` directory, run `yarn test`
3. Run the end-to-end tests. Navigate to the `e2e` folder and run `yarn e2e`
4. If you are adding a new feature, please provide documentation on how to use it.
5. Run prettier in the `tailwind-components` folder: `yarn format`

When you are ready, create a new draft PR.

For other questions or issues, please open an [new issue](https://github.com/molgenis/molgenis-emx2/issues/new/choose) or start a [discussion](https://github.com/molgenis/molgenis-emx2/discussions/new/choose).
