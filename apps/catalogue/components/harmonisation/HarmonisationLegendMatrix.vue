<script setup lang="ts">
import type { HarmonisationIconSize } from "../../interfaces/types";
const ariaId = useId();

const props = withDefaults(
  defineProps<{
    size?: HarmonisationIconSize;
  }>(),
  { size: "large" }
);
</script>
<template>
  <div
    class="flex flex-col md:flex-row md:mr-[2em] md:h-16 gap-5 justify-end md:items-center p-2 md:p-0"
  >
    <ul
      class="flex flex-col md:flex-row gap-3 list-none p-0 [&_li]:flex [&_li]:items-center [&_li]:gap-2"
    >
      <li>
        <HarmonisationStatusIcon :size="size" status="available" />
        Available
      </li>
      <li>
        <HarmonisationStatusIcon :size="size" status="unmapped" />
        No data
      </li>
    </ul>
    <VDropdown
      :aria-id="ariaId"
      :triggers="['hover', 'focus']"
      :distance="12"
      theme="tooltip"
    >
      <div class="flex gap-1 text-blue-500 hover:underline cursor-pointer">
        <BaseIcon name="info" />
        <span> About statuses </span>
      </div>
      <template #popper>
        <ul class="list-none [&_li]:flex [&_li]:gap-1">
          <li>
            <HarmonisationStatusIcon size="small" status="available" />
            <span>Available: source has data available for the variable</span>
          </li>
          <li>
            <HarmonisationStatusIcon
              size="small"
              status="unmapped"
              class="bg-white"
            />
            <span
              >No data: source does not have data available for the
              variable</span
            >
          </li>
        </ul>
      </template>
    </VDropdown>
  </div>
</template>
