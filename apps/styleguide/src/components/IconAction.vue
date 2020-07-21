<template>
  <button class="btn text-primary pl-1 pr-1" @click="onClick">
    <i :class="'fa fa-' + icon"></i>
  </button>
</template>

<script>
/** Button that is shown as a icon. Choose font-awesome icon name as 'icon' parameter to show particular icon. */
export default {
  props: {
    icon: String
  },
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

    ```jsx
    <IconAction icon="times"/>
    ```

</docs>
