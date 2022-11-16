<script setup>
import BaseIcon from "./BaseIcon.vue";
defineProps({
  label: {
    type: String,
    required: true,
  },
  icon: {
    type: String,
  },
});
</script>

<template>
  <button
    class="flex items-center gap-2 text-white transition-colors font-display text-heading-2xl hover:text-blue-800"
  >
    <BaseIcon :name="icon" />
    <span class="hidden transform translate-y-px md:block">{{ label }}</span>
  </button>
</template>
