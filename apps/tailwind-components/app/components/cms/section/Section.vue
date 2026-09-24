<script setup lang="ts">
import type { ISections } from "../../../../types/cms";
withDefaults(defineProps<ISections & { isEditable?: boolean }>(), {
  columns: 1,
});
</script>

<template>
  <div
    class="w-full py-8 justify-center items-center"
    :class="{
      'bg-form-legend': applyShadedBackground,
    }"
  >
    <div
      class="m-auto"
      :class="{
        'w-pg-section': !enableFullScreenWidth,
        'w-full': enableFullScreenWidth,
        grid: columns > 1,
        'gap-7.5': columns > 1,
        'grid-cols-2': columns === 2,
        'grid-cols-3': columns === 3,
        'grid-cols-4': columns === 4,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
