<script setup lang="ts">
import { ref } from "vue";

withDefaults(
  defineProps<{
    id: string;
    required: boolean;
  }>(),
  {
    required: false,
  }
);

const modelValue = ref("");
</script>

<template>
  <textarea
    v-model="modelValue"
    :id="id"
    :required="required"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white outline-none rounded-search-input h-30 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border p-x-5 p-y-5"
  />
</template>
