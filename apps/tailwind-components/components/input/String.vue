<script setup lang="ts">
import type { IInputProps } from "~/types/types";
const modelValue = defineModel<string | number>();
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
    class="w-full h-[56px] pr-4 font-sans text-black text-gray-300 ring-red-500 pl-3 shadow-search-input focus:outline-2 hover:shadow-search-input search-input-mobile border border-solid rounded-input"
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
