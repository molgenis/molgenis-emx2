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
        <HarmonizationStatusIcon :size="size" status="available" />
        Available
      </li>
      <li>
        <HarmonizationStatusIcon :size="size" status="unmapped" />
        No data
      </li>
    </ul>
    <VDropdown :triggers="['hover', 'focus']" :distance="12" theme="tooltip">
      <div class="flex gap-1 text-blue-500 hover:underline cursor-pointer">
        <BaseIcon name="info" />
        <span> About statuses </span>
      </div>
      <template #popper>
        <ul class="list-none [&_li]:flex [&_li]:gap-1">
          <li>
            <HarmonizationStatusIcon size="small" status="available" />
            <span>Available</span>
            <span>cohort has data available for the variable</span>
          </li>
          <li>
            <HarmonizationStatusIcon
              size="small"
              status="unmapped"
              class="bg-white"
            />
            <span>No data</span>
            <span>cohort does not have data available for the variable</span>
          </li>
        </ul>
      </template>
    </VDropdown>
  </div>
</template>
