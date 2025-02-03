<script setup lang="ts">
import { type InputProps, InputPropsDefaults } from "~/types/types";

defineProps<InputProps>();
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
    :aria-describedby="describedBy"
    class="w-full pr-16 font-sans text-black text-gray-300 h-[112px] outline-none rounded-textarea-input pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border py-2"
    :class="{
      'border-invalid text-invalid': state === 'invalid',
      'border-valid text-valid': state === 'valid',
      'border-disabled text-disabled bg-disabled': state === 'disabled',
      'bg-white': state !== 'disabled',
    }"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
