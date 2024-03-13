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
      <li v-tooltip="'cohort has data available for the variable'">
        <HarmonizationStatusIcon :size="size" status="complete" />
        available
      </li>
    </ul>
  </div>
</template>
