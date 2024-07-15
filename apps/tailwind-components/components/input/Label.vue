<script setup lang="ts">
withDefaults(
  defineProps<{
    for: string;
    label: string;
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
    hideLabel?: boolean;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
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
      'text-disabled': disabled,
      'text-invalid': hasError,
      'text-valid': valid,
    }"
  >
    {{ label }}
  </label>
</template>
