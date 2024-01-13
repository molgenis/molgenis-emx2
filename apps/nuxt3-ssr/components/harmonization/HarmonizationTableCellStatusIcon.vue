<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonizationStatus;
}>();

const tableClass = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "";
    case "partial":
      return "bg-yellow-200 hover:bg-yellow-800";
    case "complete":
      return "bg-green-500 hover:bg-blue-700";
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
      return "text-yellow-800 fill-current hover:text-white";
    case "complete":
      return "text-green-800 fill-current hover:text-white";
  }
});
</script>
<template>
  <div
    class="absolute inset-x-0 -top-px -bottom-px justify-center items-center inline-flex"
    :class="tableClass + ' ' + fillClass"
  >
    <BaseIcon v-if="iconName" :name="iconName" />
  </div>
</template>
