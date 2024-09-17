<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
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

const modelValue = ref("");
</script>

<template>
  <textarea
    :id="id"
    :required="required"
    :placeholder="placeholder"
    :disabled="disabled"
    class="w-full pr-16 font-sans text-black text-gray-300 outline-none rounded-textarea-input h-60 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border py-2"
    :class="{
      'border-invalid text-invalid': hasError,
      'border-valid text-valid': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
    v-model="modelValue"
  />
</template>
