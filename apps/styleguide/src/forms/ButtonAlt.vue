<template>
  <button
    type="button"
    class="btn border-0"
    :class="light ? 'btn-outline-light' : 'btn-outline-primary'"
    @click="onClick"
  >
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon" class="mr-2 ml-0"></i>
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
