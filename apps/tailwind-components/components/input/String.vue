<script setup lang="ts">
import type {InputProps} from "~/types/types";
import {InputPropsDefaults} from "~/types/types";
withDefaults(
    defineProps<InputProps>(),(InputPropsDefaults),
);
const modelValue = defineModel<string | number>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
</script>

<template>
  <input
    :v-bind="$props"
    class="w-full h-[56px] pr-4 font-sans text-black text-gray-300 outline-none ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border border-solid rounded-input"
    :class="{
      'border-invalid text-invalid': error,
      'border-valid text-valid': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>
