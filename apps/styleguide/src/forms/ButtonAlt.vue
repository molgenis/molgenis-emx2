<template>
  <button type="button" class="btn btn-link" @click="onClick">
    <slot />
  </button>
</template>

<script>
/** Cancel button */
import ButtonAction from "./ButtonAction";

export default {
  extends: ButtonAction
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
