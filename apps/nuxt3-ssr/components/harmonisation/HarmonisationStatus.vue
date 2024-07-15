<script setup lang="ts">
import type { HarmonisationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonisationStatus;
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
    <HarmonisationStatusIcon :status="status" size="large" /> {{ label }}
  </div>
</template>
