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
      if (
        this.$slots.default &&
        this.$slots.default[0] &&
        this.$slots.default[0].text != undefined
      )
        return this.$slots.default[0].text.trim().length > 0;
      return this.$slots.default;
    },
  },
};
</script>
