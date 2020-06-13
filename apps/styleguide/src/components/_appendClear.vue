<template>
  <button
    class="btn"
    :disabled="disabled"
    :class="{
      'btn-outline-primary': !error,
      'btn-outline-danger': error
    }"
    type="button"
    title="Clear"
    @click="$emit('clear')"
    data-clear
  >
    <i class="fa fa-times">
      <span aria-hidden="true" class="sr-only">Clear</span>
    </i>
  </button>
</template>
<script>
export default {
  props: {
    disabled: Boolean,
    error: String
  }
};
</script>
