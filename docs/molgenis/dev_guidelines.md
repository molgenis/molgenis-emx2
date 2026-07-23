# Development Guidelines

## For component development

To ensure consistency in the MOLGENIS interfaces, we would also like components to follow the same structure. Please follow these guidelines when developing components or creating new ones.

### 1. Component files start with the script

The structure of vue files should start with the `script` element first, and then the `template`. If styles are used in a component, then place them at the end of the file in the `style` block. This follows the common Vue 3 `<script setup>` convention.

```vue
<!-- MyComponent.vue -->
<script setup lang="ts">
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

### 3. All typescript interfaces are defined in the component file

Component interfaces should have a clear and unique name.

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

Good semantic html practices covers a lot of areas. In principle, it is important to make sure the appropriate elements are used so that HTML elements properly render and are accesible. For example, it is fairly common to see buttons that redirects users to another page. Buttons should only perform an action and not act as link. Instead, it would be better to use the anchor element and style it as a button.

```diff
- <button @click="window.location.href='....'">Get started</button>
+ <a href="path/to/some/page" class="btn btn-primary">Get started</a>
```

For additional information and examples, please consult the [ARIA Patterns guide](https://www.w3.org/WAI/ARIA/apg/patterns/) and the [a11y project](https://www.a11yproject.com).

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
