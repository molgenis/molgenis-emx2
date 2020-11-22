<template>
  <button
    class="btn"
    :disabled="disabled"
    :class="{
      'btn-outline-primary': !error,
      'btn-outline-danger': error,
    }"
    type="button"
    title="Add"
    @click="$emit('add')"
    data-clear
  >
    <i class="fa fa-plus">
      <span aria-hidden="true" class="sr-only">Plus</span>
    </i>
  </button>
</template>
<script>
export default {
  props: {
    disabled: Boolean,
    error: String,
  },
};
</script>
