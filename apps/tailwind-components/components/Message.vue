<template>
  <div
    class="p-3 font-bold flex items-center rounded-input"
    :class="{
      'bg-invalid text-required': type == 'invalid',
      'bg-valid text-valid': type == 'valid',
      'bg-default': type == 'valid',
    }"
  >
    <BaseIcon name="info"></BaseIcon><slot></slot>
  </div>
</template>

<script setup lang="ts">
import type { InputState } from "~/types/types";

withDefaults(
  defineProps<{
    type: InputState;
  }>(),
  {
    type: "invalid",
  }
);
</script>
