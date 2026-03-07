<script setup lang="ts">
withDefaults(
  defineProps<{
    fill?: string;
    markerType?: "circle" | "square";
  }>(),
  {
    fill: "#3182bd",
    markerType: "circle",
  }
);
</script>

<template>
  <svg width="16" height="16" view-box="0 0 16 16" preserveXminYmin="true">
    <circle
      v-if="markerType === 'circle'"
      cx="8"
      cy="8"
      r="8"
      :fill="fill"
      stroke-width="none"
    />
    <rect
      v-if="markerType === 'square'"
      x="0"
      y="0"
      width="16"
      height="16"
      :fill="fill"
      stroke-width="none"
    />
  </svg>
</template>
