<template>
  <button
    type="button"
    class="btn border-0"
    :class="light ? 'btn-outline-light' : 'btn-outline-primary'"
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
  props: {
    light: { type: Boolean, default: false },
  },
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
