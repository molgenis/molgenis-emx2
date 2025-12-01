<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    errorCount: number;
  }>(),
  {
    errorCount: 0,
  }
);
</script>
<template>
  <div
    class="inline-flex h-6 w-6 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
  >
    <span>{{ errorCount > 9 ? "9+" : errorCount }}</span>
    <span class="sr-only">
      error{{ errorCount > 1 || errorCount === 0 ? "s" : "" }} in
      {{ label }}
    </span>
  </div>
</template>
