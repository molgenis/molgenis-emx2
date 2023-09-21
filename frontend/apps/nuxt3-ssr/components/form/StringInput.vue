<script setup lang="ts">
import { ref } from "vue";

withDefaults(
  defineProps<{
    id: string;
    required: boolean;
    hasError?: boolean;
  }>(),
  {
    required: false,
    hasError: false,
  }
);

const modelValue = ref("");
</script>

<template>
  <input
    v-model="modelValue"
    type="text"
    :id="id"
    :required="required"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white outline-none rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border"
    :class="{ 'border-red-500 text-red-500': hasError }"
  />
</template>
