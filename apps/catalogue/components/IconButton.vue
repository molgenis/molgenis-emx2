<script setup>
defineProps({
  label: {
    type: String,
    required: false,
  },
  icon: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <button
    class="flex items-center justify-center transition-colors w-11 h-11 hover:text-blue-800"
  >
    <span><BaseIcon :name="icon" /></span>
    <span class="sr-only" v-if="label">{{ label }}</span>
  </button>
</template>
