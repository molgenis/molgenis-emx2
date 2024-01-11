<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonizationStatus;
}>();

const label = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "No data";
    case "partial":
      return "Partial";
    case "complete":
      return "Completed";
  }
});
</script>

<template>
  <div class="flex items-center gap-2">
    <HarmonizationStatusIcon :status="status" /> {{ label }}
  </div>
</template>
