<script setup lang="ts">
import type { ISections } from "../../../../types/cms";
withDefaults(defineProps<ISections & { isEditable?: boolean }>(), {
  columns: 1,
  enableTopPadding: true,
  enableBottomPadding: true,
  enableFullScreenWidth: false,
});
</script>

<template>
  <div
    class="w-full justify-center items-center"
    :class="{
      'bg-form-legend': applyShadedBackground,
      'pt-8': enableTopPadding,
      'pb-8': enableBottomPadding,
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
