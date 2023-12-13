<script setup lang="ts">
import type { HarmonizationIconSize } from "../../interfaces/types";

const props = withDefaults(
  defineProps<{
    size: HarmonizationIconSize;
  }>(),
  { size: "large" }
);
</script>
<template>
  <div class="flex">
    <div class="flex items-center h-16">
      <HarmonizationStatusIcon :size="size" status="complete" class="mr-1" />
      Completed
      <HarmonizationStatusIcon
        :size="size"
        status="partial"
        class="mr-1 ml-6"
      />
      Partial
      <HarmonizationStatusIcon
        :size="size"
        status="unmapped"
        class="mr-1 ml-6"
      />
      No Data
      <a class="ml-6 mr-8 hover:underline text-blue-500 cursor-help"
        >About statuses</a
      >
    </div>
  </div>
</template>
