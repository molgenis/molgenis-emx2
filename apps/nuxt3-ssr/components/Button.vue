<!-- eslint-disable vue/multi-word-component-names -->
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
    class="flex gap-1 tracking-wider text-white font-display text-heading-2xl"
  >
    <span v-if="icon"><BaseIcon :name="icon" /></span>
    <span>{{ label }}</span>
  </button>
</template>
