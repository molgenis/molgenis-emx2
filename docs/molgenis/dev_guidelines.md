# Development Guidelines

## General

### We minimize the use of in-line code comments

Use of in-line code comments should be used sparingly. For example, when citing a source or to explain why a certain mechanism is implemented. For Java, the preferred solution is to split code into separate methods and use Javadoc instead of needing in-line comments (though exceptions can be made when deemed better).

### Whenever possible, we split large code changes into multiple small PRs

To ensure code reviewing stays clear and not too much work, we keep PRs small and split large code changes (whenever possible) over multiple PRs.
One could for example use [stacked pull requests](https://docs.github.com/en/pull-requests/get-started/about-stacked-prs) to achieve this as well. 

## For frontend development

To ensure consistency in the MOLGENIS interfaces, frontend components must follow the same structure. Please follow these guidelines when developing components or creating new ones.

### 1. Component files start with the script

The structure of vue files should start with the `script` element first, and then the `template`. If styles are used in a component, then place them at the end of the file in the `style` block. This follows the common Vue 3 `<script setup>` convention.

```vue
<!-- MyComponent.vue -->
<script setup lang="ts">
...
</script>

<template>...</template>

<style>
...
</style>
```

Note: older apps and code may have deviating orders as they have not all been updated to this convention

### 2. All scripts use composition API and have typescript enabled

We prefer to use the [composition API](https://vuejs.org/api/composition-api-setup.html) for all EMX2 components, and Typescript must be enabled.

```vue
<script setup lang="ts">
import { ref } from "vue";

const count = ref<number>(0);
</script>
```

Make sure the `tsconfig.json` file is available in your app and typescript is enabled in your IDE.

### 3. All typescript interfaces are defined in the component file

Component interfaces should have a clear and unique name.

```vue
<script setup lang="ts">
interface IComponent {
  count: number;
}

const props = withDefaults(defineProps<IComponent>(), {
  count: 0,
});
</script>
```

If your interface is used in more than one component, then add it to the `src/types` folder. Rather than storing all interfaces in one large file, save them into themed files. For example, if you have an interface that is used in more than visualization component, then store it in `types/viz.ts`.

Interfaces that describe tables in a schema should not be written by hand. Generate them from the schema instead:

```bash
./gradlew generateTypes --args='<schemaName> <path/to/output.ts>'
```

See [Generate typescript types for an app](dev_apps.md#generate-typescript-types-for-an-app) for details.

### 4. Components are styled using the configured tailwind classes

We would like all components to work any theme. A number of classes are available in the `tailwind.config.js` file. Please use these when building components. For example, suppose you want to style text as an error. Instead of setting the color of the text to `text-red-500`, use the class `text-invalid`.

```diff
- <span class="text-red-500">Error: unable to perform some action</span>
+ <span class="text-invalid">Error: unable to perform some action</span>
```

If a specific style is not available, define it in the `assets/css/main.css` and configure it in the `tailwind.config.js` file.

### 5. All components follow good semantic HTML practices and are built with accessibility in mind

Good semantic html practices covers a lot of areas. In principle, it is important to-

- Use the appropriate HTML elements (e.g., actions are buttons, links are `<a>` elements, etc.)
- Interactive elements are accessible using a mouse and keyboard and mouse support. Hover and focus styles are clearly visible and they provide good visual indication that an element is interactive.
- Elements have sufficient color contrast and are readable in different themes
- Text elements use clear, concise language. It is recommended to avoid using technical jargon in the UI. When writing text, keep in mind that they are two terms: the internal, technical word and the public-facing term.
- Error messages clearly describe the issue and inform users how to resolve it. Do not use phrases such as "Oops! Something went wrong".

There are additional things to consider. Please refer to the [Accessibility guidelines](./dev_accessibility.md) page for more information and instructions on how to review PRs.

### 6. Naming conventions

**Types, interfaces and enums: PascalCase.**

```ts
export type TableRow = { _rowId: string };
```

**Interfaces: `I` prefix.** (House style; the broader TypeScript ecosystem has dropped this prefix.)

```ts
interface ITableSettings { ... }
```

**Prop and event bindings: camelCase.** Kebab-case only for HTML-native attributes such as `aria-*` and `data-*`.

```vue
<TableEMX2 :schemaId="schemaId" @rowSelected="onRowSelect" />
```

### 7. Refs with a fixed set of values are explicitly typed

When a ref may only hold a restricted set of values, pass those values as a union of string literals in the type argument, instead of relying on type inference:

```ts
const layout = ref<"list" | "grid">("grid");
```

Without the type argument, `ref("grid")` is inferred as `Ref<string>`, so any string can be assigned without a compile error. With the union type, the compiler rejects everything except the listed values — a lightweight alternative to an enum, and the set of allowed values is documented right where the ref is declared.

If the same set of values is used in more than one component, do not repeat the union inline. Define it as a named type in a shared types file and import it, so other components can use it without casting (`as "list" | "grid"`):

```ts
// types/layout.ts
export type Layout = "list" | "grid";

// in the component
const layout = ref<Layout>("grid");
```

When a plain string (for example from a route query or setting) must be narrowed to such a type at runtime, use a type guard or assertion function instead of a cast. See `apps/tailwind-components/app/utils/typeUtils.ts` for examples.

### 8. Vitest file extensions are `.test.ts` and for Playwright files `.spec.ts`

Unit tests, run by Vitest use the `.test.ts` extension.
End-to-end / Integration tests are run by Playwright and use `.spec.ts`.

### 9. A component pads only inside a border or background it draws

A component adds padding only inside a border or background that it draws itself. It sets no margin on its root element, and no padding on an edge where it draws nothing. The parent sets the space between its children, with `gap`, `space-y` or a grid.

```diff
  <!-- the card: padding inside its own border, no margin -->
- <div class="mt-5 p-12">...</div>
+ <div class="p-5 border rounded-base">...</div>

  <!-- the parent: the space between the cards -->
+ <ul class="grid gap-5">...</ul>
```

A component that follows this rule lines up with its neighbours wherever it is placed. A table header with top padding, for example, sits lower than the field label beside it.
