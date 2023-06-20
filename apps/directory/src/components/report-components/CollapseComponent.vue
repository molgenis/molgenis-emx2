<template>
  <details >
    <summary @click="open = !open">{{ openText }}</summary>
    <slot />
  </details>
</template>

<script>
export default {
  data: function () {
    return {
      open: false,
    };
  },
  computed: {
    openText() {
      return this.open ? "Hide details" : "Show details";
    },
  },
};
</script>
