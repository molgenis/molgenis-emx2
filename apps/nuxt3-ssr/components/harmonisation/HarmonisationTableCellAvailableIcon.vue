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
    <BaseIcon v-if="status === 'available'" name="check" />
    <span class="sr-only">{{ status }}</span>
  </div>
</template>
