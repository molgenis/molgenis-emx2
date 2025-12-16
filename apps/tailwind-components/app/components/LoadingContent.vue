<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import Message from "~/components/Message.vue";
import type { AsyncDataRequestStatus } from "#app";
import type { ProcessStatus } from "../../../metadata-utils/src/generic";

withDefaults(
  defineProps<{
    id: string;
    status: ProcessStatus | AsyncDataRequestStatus;
    loadingText: string;
    errorText: string | undefined;
    showSlotOnError?: boolean;
  }>(),
  {
    showSlotOnError: true,
  }
);
</script>

<template>
  <!-- nothing if: status === 'UNKNOWN' || status === 'idle' -->
  <div
    class="flex justify-center items-center text-heading-xl gap-5"
    v-if="status === 'RUNNING' || status === 'pending'"
  >
    <BaseIcon name="progress-activity" class="animate-spin" :width="40" />
    <p>{{ loadingText }}</p>
  </div>
  <Message
    :id="`${id}-error`"
    class="my-2"
    :invalid="true"
    v-else-if="status === 'ERROR' || status === 'error'"
  >
    <span>{{ errorText }}</span>
  </Message>
  <slot
    v-if="
      status === 'DONE' ||
      status === 'INVALID' ||
      status === 'success' ||
      (showSlotOnError && (status === 'ERROR' || status === 'error'))
    "
  />
</template>
