<template>
  <textarea
    v-model="modelValue"
    :id="id"
    :required="required"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white outline-none rounded-textarea-input h-60 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border py-2"
    :class="{ 'border-red-500 text-red-500': hasError }"
  />
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    required?: boolean;
    hasError?: boolean;
  }>(),
  {
    required: false,
    hasError: false,
  }
);

const modelValue = ref("");
</script>