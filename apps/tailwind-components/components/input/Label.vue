<script setup lang="ts">
defineProps<{
  invalid?: boolean;
  valid?: boolean;
  disabled?: boolean;
  required?: boolean;
  hideLabel?: boolean;
}>();
</script>

<template>
  <label
    class="pl-3 text-body-base"
    :class="{
      'sr-only': hideLabel,
      'text-disabled': disabled,
      'text-invalid': invalid,
      'text-valid': valid,
      'after:content-required after:text-required': required,
    }"
  >
    <slot></slot>
  </label>
</template>
