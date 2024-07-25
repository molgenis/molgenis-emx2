# MOLGENIS-EMX2 Component library

The `tailwind-components` library is a reimplementation of the existing emx2 components using nuxt and tailwind. This allows us to style build interfaces using a standard set of components to ensure all interfaces have a consistent layout and design.

## About the components

All components are stored in the `components` folder and stories are located in `pages`. To view the components, take a look at the [components showcase](https://emx2.dev.molgenis.org/apps/tailwind-components) or run the `yarn dev` command.

In the component folder, we've arranged the components into categories. To use the components, you will need to prefix the component name using the name of the subfolder. For example, if you wanted to use the checkbox component (`components/input/Checkbox.vue`), you would write the component as `<InputCheckbox>`. There are some folders that do not to be prefixed with the subfolder. This is described in the table below.

| Folder | Prefix | Description |
|:-------|:------|:------------|
| display | `<Display*>` | these components can be used to specify the presentation of content |
| filter | `<Filter*>` | these components are used to reduce content by user specified criteria |
| global | --- | these components are globally available (path prefix is not needed) |
| input | `<Input*>` | these components are primarily used in forms |
| viz | --- | a suite of visualization components built using D3 (path prefix is not needed) |

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

## Component development guidelines

To ensure consistency in the MOLGENIS interfaces, we would also like components to follow the same structure. Please follow these guidelines when developing components or creating new ones

### 1. Component files should start with the template

All component files should start with the template markup first and then the script. This allows other developers to clearly see the aim of the component. Make sure vue component files are structured like so.

```vue
<!-- MyComponent.vue -->
<template>
  ...
</template>

<script>
  ...
</script>
```

### 2. All scripts should use composition API and have typescript enabled

We prefer to use the [composition API](https://vuejs.org/api/composition-api-setup.html) in emx2 components. Typescript should also be enabled and implemented everywhere.

```vue
<script setup lang="ts">
import { ref } from "vue";

const count = ref<number>(0);
</script>
```

### 3. All typescript interfaces should be defined in the component file

If your component uses a new interface, please define it in the component and prefix the interface with `I`.

```vue
<script setup lang="ts">

interface IComponent {
  count: number
}

const props = withDefaults(
  defineProps<IComponent>(),
  {
    count: 0
  }
);

</script>
```

If your interface is used in more than one component, then add it to the `types` folder. Instead of storing all interfaces in one file, save them into themed type files. For example, if you have an interface that is used in more than visualization component, then store it in `types/viz.ts`.

### 4. Components should be styled using the configured tailwind classes

We would like all components to work any theme. A number of classes are available in the `tailwind.config.js` file. Please use these when building components. For example, suppose you want to style text as an error. Instead of setting the color of the text to `text-red-500`, use the class `text-invalid`.

```diff
- <span class="text-red-500">Error: unable to perform some action</span>
+ <span class="text-invalid">Error: unable to perform some action</span>
```

If a specific style is not available, define it in the `assets/css/main.css` and configure it in the `tailwind.config.js` file.

### 5. All components must follow good semantic HTML practices and should be built with accessibility in mind

Good semantic html practices covers a lot of areas. In principle, it is important to make sure the appropriate elements are used so that HTML elements properly render and are accesible. For example, it is fairly common to see buttons that redirect users to another page. In this instance, buttons should perform only an action and not act as link. Instead, use the anchor element and style it as a button.

```diff
- <button @click="window.location.href='....'">Get started</button>
+ <a href="path/to/some/page" class="btn btn-primary">Get started</a>
```

For additional information and examples, please consult the [ARIA Patterns guide](https://www.w3.org/WAI/ARIA/apg/patterns/) and the [a11y project](https://www.a11yproject.com).

## Getting started

To work on the component library locally, clone the molgenis-emx2 repository and create a new branch.

```bash
git clone https://github.com/molgenis/molgenis-emx2
git switch -c <my-branch>
```

### Setup

We use [yarn workspaces](https://yarnpkg.com/features/workspaces) to manage dependencies across all applications. On the first time, install the dependencies.

```bash
yarn install
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

### Creating a new PR

We warmly welcome PRs to the molgenis-emx2 repository. To get started, please see our Contributing Guidelines (tbd). Before you create a new PR, please complete the following steps.

1. Make sure all typescript issues are resolved
2. If applicable, write tests and make sure all tests pass. In the `tailwind-components` directory, run `yarn test`
3. Run the end-to-end tests. Navigate to the `e2e` folder and run `yarn e2e`
4. If you are adding a new feature, please provide documentation on how to use it.
5. Run prettier in the `tailwind-components` folder: `yarn format`

When you are ready, create a new draft PR.

For other questions or issues, please open an [new issue](https://github.com/molgenis/molgenis-emx2/issues/new/choose) or start a [discussion](https://github.com/molgenis/molgenis-emx2/discussions/new/choose).
