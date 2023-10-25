<script setup lang="ts">
import { HarmonizationStatus } from "~/interfaces/types";

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
  <div
    class="w-8 h-8 p-1 justify-center items-center inline-flex"
    :class="tableClass"
  >
    <BaseIcon v-if="iconName" :name="iconName" :class="fillClass" />
  </div>
</template>
