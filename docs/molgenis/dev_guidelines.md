# Development Guidelines

## For component development

To ensure consistency in the MOLGENIS interfaces, we would also like components to follow the same structure. Please follow these guidelines when developing components or creating new ones.

### 1. Component files should start with the template

All component files should start with the script first, then the template, and finally the style block. This follows the common Vue 3 `<script setup>` convention.

```vue
<!-- MyComponent.vue -->
<script>
  ...
</script>

<template>
  ...
</template>

<style>
  ...
</style>
```

Note: older apps and code may have deviating orders as they have not all been updated to this convention

### 2. All scripts use composition API and have typescript enabled

We prefer to use the [composition API](https://vuejs.org/api/composition-api-setup.html) for all EMX2 components. Typescript should also be enabled.

```vue
<script setup lang="ts">
import { ref } from "vue";

const count = ref<number>(0);
</script>
```

Make sure the `tsconfig.json` file is available in your app and typescript is enabled in your IDE.

### 3. All typescript interfaces should be defined in the component file

Component interfaces should have a clear and unique name. Interfaces should also be prefixed with `I`.

```vue
<script setup lang="ts">

interface IComponent {
  count: number;
}

const props = withDefaults(
  defineProps<IComponent>(),
  {
    count: 0
  }
);

</script>
```

If your interface is used in more than one component, then add it to the `src/types` folder. Rather than storing all interfaces in one large file, save them into themed files. For example, if you have an interface that is used in more than visualization component, then store it in `types/viz.ts`.

### 4. Components should be styled using the configured tailwind classes

We would like all components to work any theme. A number of classes are available in the `tailwind.config.js` file. Please use these when building components. For example, suppose you want to style text as an error. Instead of setting the color of the text to `text-red-500`, use the class `text-invalid`.

```diff
- <span class="text-red-500">Error: unable to perform some action</span>
+ <span class="text-invalid">Error: unable to perform some action</span>
```

If a specific style is not available, define it in the `assets/css/main.css` and configure it in the `tailwind.config.js` file.

### 5. All components must follow good semantic HTML practices and should be built with accessibility in mind

Good semantic html practices covers a lot of areas. In principle, it is important to make sure the appropriate elements are used so that HTML elements properly render and are accesible. For example, it is fairly common to see buttons that redirects users to another page. Buttons should only perform an action and not act as link. Instead, it would be better to use the anchor element and style it as a button.

```diff
- <button @click="window.location.href='....'">Get started</button>
+ <a href="path/to/some/page" class="btn btn-primary">Get started</a>
```

For additional information and examples, please consult the [ARIA Patterns guide](https://www.w3.org/WAI/ARIA/apg/patterns/) and the [a11y project](https://www.a11yproject.com).

### 6. Other guidelines

Types of refs:
```const myprop = ref<"option1" | "option2">("option2")```

### 7. Naming conventions

**Type-like identifiers (type aliases, interfaces, enums) are PascalCase.**

```diff
- export type tableRow = { ... };
+ export type TableRow = { ... };
```

This follows the standard TypeScript convention (typescript-eslint `naming-convention`, selector `typeLike`). Interfaces may additionally carry the `I` prefix (see guideline 3), which is also PascalCase: `ITableSettings`. This rule is enforced by ESLint (`apps/eslint.config.mjs`); a handful of pre-existing lowercase type names are grandfathered there and should be renamed over time, not imitated.

**Client-side internal data properties are prefixed with a single underscore.**

Properties that the frontend synthesizes onto row/data objects (i.e. that do not correspond to a user-defined column) use a `_` prefix, for example `_rowId`. This is safe and unambiguous because:

- EMX2 column names must start with a letter (see `COLUMN_NAME_REGEX` in the backend `Constants.java`), so a `_`-prefixed property can never collide with real data.
- The GraphQL API already uses this namespace for its own meta fields (`_agg`, `_groupBy`, `_settings`, `_session`, ...).

Note that this does not conflict with the "leading underscore means unused" idiom: that idiom (and how linters interpret it, e.g. `argsIgnorePattern: "^_"`) applies to *bindings* such as function parameters and variables, never to object property names.

For comparison: system columns that are physically stored in tables use the `mg_` prefix (`mg_insertedBy`, `mg_draft`, ...); the `_` prefix is reserved for values that exist only in the API or the client.

## For java development

### We don't use var

Within our code base we decided to not use the java 'var' syntax but always use explicity typing.

### We don't tear down testing schemas

Any schemas created in tests through `TestDatabaseFactory` aren't removed after the tests are finished.
This to make debugging more easily.
Instead, during a `@BeforeAll`/`@BeforeEach`, the relevant test schema is dropped and created again.

Example:
```java
class MyClassTest {
  static Database database;

  @BeforeAll
  public static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists("linkedSchemaThatMustBeRemovedFirst"); // Add this if needed.
    database.dropCreateSchema("mySchemaName");
    database.dropCreateSchema("linkedSchemaThatMustBeRemovedFirst");
  }
  
  // No `@AfterAll` that removes the schemas!
  
  @Test
  void testSomething() {
    // Do stuff.
  }
}
```
