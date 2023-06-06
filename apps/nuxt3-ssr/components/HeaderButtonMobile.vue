<script setup>
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
    class="flex items-center gap-2 text-white transition-colors font-display text-heading-2xl">
    <BaseIcon :name="icon" />
    <span class="transform translate-y-px block">{{ label }}</span>
  </button>
</template>
