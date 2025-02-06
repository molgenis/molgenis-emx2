<script setup lang="ts">
import type { IInputProps } from "~/types/types";
const modelValue = defineModel<string | number>();
defineProps<
  IInputProps & {
    type?: string;
  }
>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
</script>

<template>
  <input
    :v-bind="$props"
    :aria-describedby="describedBy"
    :type="type || 'text'"
    :placeholder="placeholder"
    class="w-full h-[56px] pr-4 font-sans text-black text-gray-300 outline-none ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border border-solid rounded-input"
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
