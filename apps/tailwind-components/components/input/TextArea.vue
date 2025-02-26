<script setup lang="ts">
import { type IInputProps } from "~/types/types";
defineProps<IInputProps>();
const modelValue = defineModel<string>();
const emit = defineEmits([
  "focus",
  "blur",
  "input",
  "error",
  "update:modelValue",
]);
</script>

<template>
  <textarea
    :id="id"
    :placeholder="placeholder"
    :disabled="disabled"
    class="w-full h-[112px] pl-3 pr-16 py-2 border outline-none rounded-textarea-input"
    :class="{
      'bg-input border-valid text-valid': valid && !disabled,
      'bg-input border-invalid text-invalid': invalid && !disabled,
      'border-disabled text-disabled bg-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'bg-input text-current hover:border-input-hover focus:border-input-focused':
        !disabled && !invalid && !valid,
    }"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
