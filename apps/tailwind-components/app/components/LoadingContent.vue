<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import Message from "~/components/Message.vue";

defineProps<{
  id: string;
  isLoading: boolean;
  loadingText: string;
  errorText: string | undefined;
}>()
</script>

<template>
  <div v-if="isLoading">
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
      v-else-if="errorText"
  >
    <span>{{ errorText }}</span>
  </Message>
  <slot v-else></slot>
</template>