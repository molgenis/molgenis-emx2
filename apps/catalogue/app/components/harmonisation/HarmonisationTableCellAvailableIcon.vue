<script setup lang="ts">
import type { HarmonisationStatus } from "../../../interfaces/types";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import { computed } from "vue";

const props = defineProps<{
  status: HarmonisationStatus;
}>();

const statusClasses = computed(() => {
  switch (props.status) {
    case "available":
      return "bg-label-available text-label-available hover:bg-label-available-hover group-hover:bg-label-available-hover";
    default:
      return "";
  }
});
</script>
<template>
  <div
    :data-status="status"
    class="absolute inset-x-0 -top-px -bottom-px justify-center items-center inline-flex group"
    :class="statusClasses"
  >
    <BaseIcon v-if="status === 'available'" name="check" :class="statusClasses" />
    <span class="sr-only">{{ status }}</span>
  </div>
</template>
