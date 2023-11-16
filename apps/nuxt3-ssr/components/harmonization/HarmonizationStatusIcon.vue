<script setup lang="ts">
import type {
  HarmonizationIconSize,
  HarmonizationStatus,
} from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    status: HarmonizationStatus;
    size: HarmonizationIconSize;
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
    class="p-1 justify-center items-center inline-flex"
    :class="`w-${tailwindIconSize} h-${tailwindIconSize} ${tableClass}`"
  >
    <BaseIcon v-if="iconName" :name="iconName" :class="fillClass" />
  </div>
</template>
