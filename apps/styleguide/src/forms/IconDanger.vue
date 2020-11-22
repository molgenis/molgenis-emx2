<template>
  <button class="btn text-danger pl-1 pr-1" @click="onClick">
    <i :class="'fa fa-' + icon"></i>
  </button>
</template>

<script>
/** Button that is shown as a icon */
export default {
  props: {
    icon: String,
  },
  methods: {
    onClick() {
      /** emitted on click */
      this.$emit("click");
    },
  },
};
</script>

<docs>
    Example

    ```
    <template>
        <IconDanger icon="times" @click="alert('clicked')"/>
    </template>
    <script>
        export default {
            methods: {
                alert(text) {
                    alert(text)
                }
            }
        }
    </script>
    ```

</docs>
