<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

const props = defineProps<{
  status: HarmonizationStatus;
}>();

const availableStatuses = ['partial', 'complete'];

const recodedStatus = computed(() => {
  if (availableStatuses.includes(props.status)) {
    return "available";
  }
  return props.status;
})

</script>
<template>
  <div
    :data-status="recodedStatus"
    class="absolute inset-x-0 -top-px -bottom-px justify-center items-center inline-flex
    data-[status=available]:bg-blue-500
    data-[status=available]:fill-white
    "
  >
    <BaseIcon v-if="recodedStatus === 'available'" name="check" />
    <span class="sr-only">{{ status }}</span>
  </div>
</template>
