<template>
  <button
    class="btn bg-transparent"
    :disabled="disabled"
    type="button"
    title="Clear"
    @click="$emit('clear')"
    :class="{
      'text-primary': !error,
      'text-danger': error
    }"
    data-clear
    tabindex="-1"
  >
    <i class="fa fa-times">
      <span aria-hidden="true" class="sr-only">Clear</span>
    </i>
  </button>
</template>

<style scoped>
.btn {
  margin-left: -38px;
  z-index: 100;
}
</style>

<script>
export default {
  props: {
    disabled: Boolean,
    error: String
  }
};
</script>
