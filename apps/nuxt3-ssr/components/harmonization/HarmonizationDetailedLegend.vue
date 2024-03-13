<script setup lang="ts">
import type { HarmonizationIconSize } from "../../interfaces/types";

const props = withDefaults(
  defineProps<{
    size?: HarmonizationIconSize;
  }>(),
  { size: "large" }
);
</script>
<template>
  <div class="flex flex-row justify-end items-center h-16 mr-[2em]">
    <ul
      class="flex justify-end items-center gap-3 mr-3 list-none [&_li]:flex [&_li]:items-center [&_li]:gap-2"
    >
      <li v-tooltip="'cohort was able to fully map to the harmonized variables'">
        <HarmonizationStatusIcon :size="size" status="complete" />
        Completed
      </li>
      <li v-tooltip="'cohort was able to partially map to the harmonized variable'">
        <HarmonizationStatusIcon :size="size" status="partial" />
        Partial
      </li>
    </ul>
  </div>
</template>
