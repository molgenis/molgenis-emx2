<template>
  <div
    class="p-3 font-bold flex items-center rounded-input"
    :class="{
      'bg-invalid text-required': type == 'error',
      'bg-valid text-valid': type == 'valid',
      'bg-default': type == 'valid',
    }"
  >
    <BaseIcon name="info"></BaseIcon><slot></slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    type: "error" | "warning" | "valid";
  }>(),
  {
    type: "error",
  }
);
</script>
