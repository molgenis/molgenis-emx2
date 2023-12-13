<template>
  <div :class="classNames">
    <div class="dashboard-content">
      <!-- main dashboard content -->
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  // specify the left and right padding of the default slot
  horizontalPadding: {
    // `0:5`
    type: Number,
    // `1`
    default: 1,
    validator: (value) => {
      return value >= 0 && value <= 5;
    },
  },
  // specify the top and bottom padding of the default slote
  verticalPadding: {
    // `0:5`
    type: Number,
    // `1`
    default: 1,
    validator: (value) => {
      return value >= 0 && value <= 5;
    },
  },
});

const classNames = computed(() => {
  return `dashboard-container padding-h-${props.horizontalPadding} padding-v-${props.verticalPadding}`;
});
</script>

<style lang="scss">
.dashboard-container {
  background-color: $gray-050;
  padding: 2em 0;

  .dashboard-content {
    display: grid;
    margin: 0 auto;
    padding: 2em 0;
    gap: 2em;
  }
}
</style>
