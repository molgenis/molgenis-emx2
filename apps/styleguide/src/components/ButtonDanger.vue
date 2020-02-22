<template>
  <button type="button" class="btn btn-danger" @click="onClick">
    <slot />
    {{testvalue}}
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
<ButtonDanger v-on:click="alert('hello');">Dangerous actions</ButtonDanger>
```

</docs>
