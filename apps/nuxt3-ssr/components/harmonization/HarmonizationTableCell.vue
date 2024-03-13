<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

// Note: component similar to HarmonizationStatusIcon.vue but with different due to table cell styling details

const props = defineProps<{
  status: HarmonizationStatus;
}>();

const tableClass = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "";
    case "partial":
      return "bg-yellow-200";
    case "complete":
      return "bg-green-500";
  }
});

const iconName = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "";
    case "partial":
      return "percent";
    case "complete":
      return "check";
  }
});

const fillClass = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "";
    case "partial":
      return "text-yellow-800 fill-current";
    case "complete":
      return "text-green-800 fill-current";
  }
});
</script>
<template>
  <td class="text-center" :class="tableClass">
    <BaseIcon v-if="iconName" :name="iconName" :class="fillClass" />
  </td>
</template>
