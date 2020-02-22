<template>
  <button type="button" class="btn btn-primary" @click="onClick">
    <slot />
  </button>
</template>

<script>
/** Button that is shown as a primary action */
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

```jsx
<ButtonAction v-on:click="alert('hello');">Action</ButtonAction>
```

</docs>
