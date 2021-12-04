<template>
  <button
    class="btn bg-transparent"
    :disabled="disabled"
    type="button"
    title="Clear"
    @click="$emit('clear')"
    style="margin-left: -38px; z-index: 100"
    :class="{
      'text-primary': !error,
      'text-danger': error,
    }"
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
    error: String,
  },
};
</script>
