<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    label?: string;
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
  }
);

const modelValue = defineModel()
</script>

<template>
  <input
    :id="id"
    :required="required"
    :placeholder="placeholder"
    class="w-full pr-4 font-sans text-black text-gray-300 outline-none rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border"
    :class="{
      'border-invalid text-invalid': hasError,
      'border-valid text-valid': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
    v-model="modelValue"
  />
</template>
