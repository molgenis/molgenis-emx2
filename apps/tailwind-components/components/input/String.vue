<script setup lang="ts">
import type { IInputProps } from "../../types/types";

const modelValue = defineModel<string | number | undefined>({ required: true });

defineProps<
  IInputProps & {
    type?: string;
  }
>();
const emit = defineEmits(["focus", "blur"]);
</script>

<template>
  <input
    :id="id"
    :aria-describedby="describedBy"
    :type="type || 'text'"
    :placeholder="placeholder"
    :disabled="disabled"
    class="w-full h-[56px] pr-4 pl-3 border outline-none rounded-input"
    :class="{
      'bg-input border-valid text-valid': valid && !disabled,
      'bg-input border-invalid text-invalid': invalid && !disabled,
      'border-disabled text-disabled bg-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'bg-input text-input hover:border-input-hover focus:border-input-focused':
        !disabled && !invalid && !valid,
    }"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
