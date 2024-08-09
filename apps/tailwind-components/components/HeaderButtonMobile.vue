<template>
  <button
    class="flex items-center gap-2 text-white transition-colors font-display text-heading-2xl"
  >
    <BaseIcon :name="icon" />
    <span class="transform translate-y-px block">{{ label }}</span>
  </button>
</template>

<script setup type="ts">
defineProps<{
  label: string;
  icon?: string;
}>();
</script>
