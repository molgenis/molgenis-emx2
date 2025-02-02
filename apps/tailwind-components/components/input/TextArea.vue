<script setup lang="ts">
import type { columnValue } from "../../../metadata-utils/src/types";
import { type InputProps, InputPropsDefaults } from "~/types/types";

const props = withDefaults(
  defineProps<
    InputProps & {
      modelValue?: string;
    }
  >(),
  {
    ...InputPropsDefaults,
  }
);

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
    class="w-full pr-16 font-sans text-black text-gray-300 h-[112px] outline-none rounded-textarea-input pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border py-2"
    :class="{
      'border-invalid text-invalid': state === 'invalid',
      'border-valid text-valid': state === 'valid',
      'border-disabled text-disabled bg-disabled': state === 'disabled',
      'bg-white': state !== 'disabled',
    }"
    :value="modelValue"
    @input="onInput"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
