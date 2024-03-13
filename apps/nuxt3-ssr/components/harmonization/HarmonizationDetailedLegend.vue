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
  <div class="flex flex-row justify-end items-center h-16 mr-[2em] gap-5">
    <ul
      class="flex justify-end items-center gap-3 mr-3 list-none [&_li]:flex [&_li]:items-center [&_li]:gap-2"
    >
      <li>
        <HarmonizationStatusIcon :size="size" status="complete" />
        Completed
      </li>
      <li>
        <HarmonizationStatusIcon :size="size" status="partial" />
        Partial
      </li>
    </ul>
    <VDropdown :triggers="['hover', 'focus']" :distance="12" theme="tooltip">
      <span class="text-blue-500 hover:underline cursor-pointer"
        >About statuses</span
      >
      <template #popper>
        <ul class="pl-3 min-w-95 list-disc">
          <li>
            Completed: cohort was able to fully map to the harmonized variables
          </li>
          <li>
            Partial: cohort was able to partially map to the harmonized variable
          </li>
        </ul>
      </template>
    </VDropdown>
  </div>
</template>
