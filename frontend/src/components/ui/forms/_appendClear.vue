<template>
  <button
    class="btn bg-transparent"
    :class="{
      'text-primary': !error,
      'text-danger': error,
    }"
    data-clear
    :disabled="disabled"
    style="margin-left: -40px; z-index: 100;"
    title="Clear"
    type="button"
    @click="$emit('clear')"
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
  emits: ['clear'],
}
</script>
