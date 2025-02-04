<template>
  <div class="dashboard-chart-layout" :class="`columns-${columns}`">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    columns: 1 | 2 | 3 | 4;
  }>(),
  {
    columns: 2,
  }
);
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
