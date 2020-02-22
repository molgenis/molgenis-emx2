<template>
  <button type="button" class="btn btn-link" @click="onClick">
    <slot />
  </button>
</template>

<script>
/** Cancel button */
export default {
  methods: {
    onClick () {
      /** emitted on click */
      this.$emit('click')
    }
  }
}
</script>

<docs>
Example

```
<ButtonAlt v-on:click="action('cancel')">Cancel</ButtonAlt>
```

</docs>
