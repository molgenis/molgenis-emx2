<template>
  <div
    role="alertdialog"
    :aria-labelledby="type === 'invalid' ? id + '-error-state' : ''"
    class="p-3 font-bold flex items-center rounded-input"
    :class="{
      'bg-invalid text-required': type == 'invalid',
      'bg-valid text-valid': type == 'valid',
      'bg-default': type == 'valid',
    }"
  >
    <span v-if="type === 'invalid'" :id="`${id}-error-state`" class="sr-only"
      >error</span
    >
    <BaseIcon name="info"></BaseIcon>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import type { InputState } from "~/types/types";

withDefaults(
  defineProps<{
    id: string;
    type: InputState;
  }>(),
  {
    type: "invalid",
  }
);
</script>
