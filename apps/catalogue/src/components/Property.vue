<template>
  <div>
    <label>{{ label }}:</label>&nbsp;
    <slot v-if="hasSlot" />
    <span v-else>N/A</span>
  </div>
</template>

<script>
export default {
  props: {
    label: String,
  },
  computed: {
    hasSlot() {
      return !!this.$slots.default[0].text.length;
    },
  },
};
</script>
