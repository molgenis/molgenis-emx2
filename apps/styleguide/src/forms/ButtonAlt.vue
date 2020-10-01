<template>
  <button type="button" class="btn btn-link" @click="onClick">
    <slot />
  </button>
</template>

<script>
/** Cancel button */
export default {
  methods: {
    onClick() {
      /** emitted on click */
      this.$emit("click");
    }
  }
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
