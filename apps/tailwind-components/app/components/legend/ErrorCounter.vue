<script setup lang="ts">
import type { MaybeRef } from "vue";
import { unref } from "vue";

withDefaults(
  defineProps<{
    label: string;
    errorCount: MaybeRef<number>;
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
    <span>{{ unref(errorCount) > 9 ? "9+" : unref(errorCount) }}</span>
    <span class="sr-only">
      error{{ unref(errorCount) > 1 || unref(errorCount) === 0 ? "s" : "" }} in
      {{ label }}
    </span>
  </div>
</template>
