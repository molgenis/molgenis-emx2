<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import Message from "~/components/Message.vue";
import type {AsyncDataRequestStatus} from "#app";

defineProps<{
  id: string;
  status: AsyncDataRequestStatus;
  loadingText: string;
  errorText: string | undefined;
}>()
</script>

<template>
  <div v-if="status === 'idle'" />
  <div v-else-if="status === 'pending'">
    <BaseIcon
        name="progress-activity"
        class="animate-spin m-auto"
        :width="32"
    />
    <p>{{loadingText}}</p>
  </div>
  <Message
      :id="`${id}-error`"
      class="my-2"
      :invalid="true"
      v-else-if="status === 'error'"
  >
    <span>{{ errorText }}</span>
  </Message>
  <slot v-else></slot>
</template>