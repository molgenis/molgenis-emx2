<script setup lang="ts">
import { computed } from "vue";
import type {
  HarmonisationIconSize,
  HarmonisationStatus,
} from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    status: HarmonisationStatus;
    size: HarmonisationIconSize;
  }>(),
  { size: "large" }
);

const tailwindIconSize = computed(() => (props.size === "large" ? "8" : "5"));

const tableClass = computed(() => {
  switch (props.status) {
    case "unmapped":
      return "border ";
    case "partial":
      return "bg-yellow-200";
    case "complete":
      return "bg-green-500";
    case "available":
      return "bg-blue-500";
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
    case "available":
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
    case "available":
      return "bg-blue-500 fill-white";
  }
});
</script>
<template>
  <div
    class="p-1 justify-center items-center inline-flex"
    :class="`w-${tailwindIconSize} h-${tailwindIconSize} ${tableClass}`"
  >
    <BaseIcon v-if="iconName" :name="iconName" :class="fillClass" />
  </div>
</template>
