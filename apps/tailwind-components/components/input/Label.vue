<script setup lang="ts">
withDefaults(
  defineProps<{
    state?: InputState;
    required?: boolean;
    hideLabel?: boolean;
    for: string;
  }>(),
  {
    hideLabel: false,
  }
);
</script>

<template>
  <label
    :for="for"
    class="pl-3 text-body-base"
    :class="{
      'sr-only': hideLabel,
      'text-disabled': state === 'disabled',
      'text-invalid': state === 'invalid',
      'text-valid': state === 'valid',
      'after:content-required after:text-required': required,
    }"
  >
    <slot></slot>
  </label>
</template>
