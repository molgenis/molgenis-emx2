<template>
  <button type="button" class="btn btn-primary" @click="onClick">
    <slot />
  </button>
</template>

<script>
/** Button that is shown as a primary action */
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
        <ButtonAction v-on:click="alert('hello');">Action</ButtonAction>
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
