<template>
  <label
    class="pl-3 text-body-base"
    :class="{
      'sr-only': hideLabel,
      'text-disabled': disabled,
      'text-invalid': hasError,
      'text-valid': valid,
      'after:content-required after:text-required': required,
    }"
  >
    <slot></slot>
  </label>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    disabled?: boolean;
    valid?: boolean;
    hasError?: boolean;
    required?: boolean;
    hideLabel?: boolean;
  }>(),
  {
    disabled: false,
    hasError: false,
    valid: false,
    hideLabel: false,
  }
);
</script>
