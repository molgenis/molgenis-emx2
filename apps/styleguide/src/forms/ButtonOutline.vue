<template>
  <button
    type="button"
    :class="light ? 'btn btn-outline-light' : 'btn btn-outline-primary'"
    @click="onClick"
  >
    <i v-if="icon" :class="icon" class="mr-2 ml-0"></i>
    <slot />
  </button>
</template>

<script>
/** Cancel button */
import ButtonAlt from "./ButtonAlt";

export default {
  extends: ButtonAlt,
};
</script>

<docs>
Example

```
<template>
  <ButtonOutline v-on:click="alert('cancel clicked')">Cancel</ButtonOutline>
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
