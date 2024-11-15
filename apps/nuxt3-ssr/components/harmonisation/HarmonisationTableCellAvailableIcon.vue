<script setup lang="ts">
import type { HarmonisationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonisationStatus;
}>();
</script>
<template>
  <div
    :data-status="status"
    class="absolute inset-x-0 -top-px -bottom-px justify-center items-center inline-flex data-[status=available]:bg-blue-500 data-[status=available]:fill-white data-[status=available]:hover:bg-blue-700"
  >
    <Icon v-if="status === 'available'" name="twc:check" class="text-white" />
    <span class="sr-only">{{ status }}</span>
  </div>
</template>
