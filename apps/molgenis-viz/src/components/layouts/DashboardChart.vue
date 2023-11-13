<template>
  <div :class="classNames">
    <!-- Layout component for visualization -->
    <slot></slot>
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
  return `dashboard-box padding-h-${props.horizontalPadding} padding-v-${props.verticalPadding}`;
});
</script>

<style lang="scss">
.dashboard-box {
  box-shadow: $box-shadow;
  box-sizing: content-box;
  border-radius: 8px;
  background-color: $gray-000;
}
</style>
