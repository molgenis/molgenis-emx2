<script setup lang="ts">
import { HarmonizationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonizationStatus;
}>();

const tableClass = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "unmapped";
    case "partial":
      return "bg-orange-500";
    case "complete":
      return "bg-green-500";
  }
});

const iconName = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "";
    case "partial":
      return "info";
    case "complete":
      return "completed";
  }
});
</script>
<template>
  <td class="text-center" :class="tableClass">
    <BaseIcon v-if="iconName" :name="iconName" />
  </td>
</template>
