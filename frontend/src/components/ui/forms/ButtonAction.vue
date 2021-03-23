<template>
  <button class="btn btn-primary" type="button" @click="onClick">
    <i v-if="icon" :class="icon" />
    <slot />
  </button>
</template>

<script>
/** Button that is shown as a primary action */
export default {
  props: {
    href: String,
    icon: String,
  },
  emits: ['click'],
  methods: {
    onClick() {
      if (this.href) {
        window.open(this.href)
      } else {
        /** emitted on click */
        this.$emit('click')
      }
    },
  },
}
</script>
