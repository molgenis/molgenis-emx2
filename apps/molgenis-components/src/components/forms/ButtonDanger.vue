<template>
  <button type="button" class="btn btn-danger" @click="onClick">
    <slot />
  </button>
</template>

<script>
import ButtonAction from "./ButtonAction.vue";

/** Button that is shown as a primary action */
export default {
  extends: ButtonAction,
};
</script>

<docs>
Example

```jsx
<template>
  <ButtonDanger v-on:click="alert('danger clicked');">Dangerous actions</ButtonDanger>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      }
    }
  }
</script>
```

</docs>
