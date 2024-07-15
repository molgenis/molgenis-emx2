<template>
  <div :class="classNames">
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  columns: {
    type: Number,
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

  &.columns-2 {
    @media screen and (min-width: 1024px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  &.columns-3 {
    @media screen and (min-width: 1024px) {
      grid-template-columns: repeat(3, 1fr);
    }
  }

  &.columns-4 {
    @media screen and (min-width: 1024px) {
      grid-template-columns: repeat(4, 1fr);
    }
  }

  &.columns-5 {
    @media screen and (min-width: 1024px) {
      grid-template-columns: repeat(5, 1fr);
    }
  }
}
</style>
