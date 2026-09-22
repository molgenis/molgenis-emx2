<script setup lang="ts">
withDefaults(
  defineProps<{
    lines?: number;
  }>(),
  { lines: 4 }
);
</script>

<template>
  <div class="space-y-2 py-1" role="status" aria-label="Loading">
    <div
      v-for="n in lines"
      :key="n"
      class="h-4 rounded bg-disabled animate-pulse"
      :style="{ width: `calc(var(--skeleton-bar-base) + ${n * 8}%)` }"
    />
  </div>
</template>
