<template>
  <button
    type="button"
    class="btn btn-outline-primary border-0"
    @click="onClick"
  >
    <i v-if="icon" :class="icon" class="mr-2 ml-0"></i>
    <slot />
  </button>
</template>

<script>
/** Cancel button */
import ButtonAction from "./ButtonAction";

export default {
  extends: ButtonAction,
};
</script>

<docs>
Example

```
<template>
  <ButtonAlt v-on:click="alert('cancel clicked')">Cancel</ButtonAlt>
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
