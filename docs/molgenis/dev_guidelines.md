# Development Guidelines

## For component development

To ensure consistency in the MOLGENIS interfaces, we would also like components to follow the same structure. Please follow these guidelines when developing components or creating new ones.

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


