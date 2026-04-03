<template>
  <div :class="classNames">
    <!-- Dashboard row content or visualisations wrapped in DashboardChart layout component -->
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  // The number of columns from 1 to 4
  columns: {
    type: Number,
    // `2`
    default: 2,
    validator: (value) => {
      return value >= 1 && value <= 4;
    },
  },
});

const classNames = computed(() => {
  return `dashboard-chart-layout columns-${props.columns}`;
});
</script>

<style lang="scss">
.dashboard-chart-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2em;

  @media (min-width: 1024px) {
    &.columns-2 {
      grid-template-columns: repeat(2, 1fr);
    }

    &.columns-3 {
      grid-template-columns: repeat(3, 1fr);
    }

    &.columns-4 {
      grid-template-columns: repeat(4, 1fr);
    }
  }
}
</style>
