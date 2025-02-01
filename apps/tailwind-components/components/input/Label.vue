<script setup lang="ts">
import { type InputProps, InputPropsDefaults } from "~/types/types";

withDefaults(
  defineProps<
    InputProps & {
      required?: boolean;
      hideLabel?: boolean;
    }
  >(),
  {
    ...InputPropsDefaults,
    hideLabel: false,
  }
);
</script>

<template>
  <label
    class="pl-3 text-body-base"
    :class="{
      'sr-only': hideLabel,
      'text-disabled': disabled,
      'text-invalid': error,
      'text-valid': valid,
      'after:content-required after:text-required': required,
    }"
  >
    <slot></slot>
  </label>
</template>
