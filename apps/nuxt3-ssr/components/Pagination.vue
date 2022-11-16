<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import BaseIcon from "./BaseIcon.vue";

defineProps({
  currentPage: {
    type: Number,
    required: true,
  },
  totalPages: {
    type: Number,
    required: true,
  },
});
</script>

<template>
  <nav class="py-12.5 flex items-center justify-center font-display text-heading-xl">
    <a href="#" class="rounded-full bg-blue-800 text-white h-15 w-15 flex justify-center">
      <BaseIcon name="caret-left" width="24" />
    </a>
    <div class="px-5 text-gray-400">Page</div>
    <div class="sm:px-12 px-6 border rounded-full text-blue-800 drop-shadow h-15 flex items-center">
      {{ currentPage }}
    </div>
    <div class="px-5 text-gray-400 whitespace-nowrap">OF {{ totalPages }}</div>
    <a href="#" class="rounded-full bg-blue-800 text-white h-15 w-15 flex justify-center">
      <BaseIcon name="caret-right" width="24" />
    </a>
  </nav>
</template>
