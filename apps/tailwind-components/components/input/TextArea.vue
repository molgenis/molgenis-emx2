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
    class="w-full pr-16 font-sans text-black text-gray-300 h-[112px] outline-none rounded-textarea-input pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border py-2"
    :class="{
      'border-invalid text-invalid border-2': invalid,
      'border-valid text-valid border-2': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
