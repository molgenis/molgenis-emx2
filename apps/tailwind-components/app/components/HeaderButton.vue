<script setup lang="ts">
import BaseIcon from "./BaseIcon.vue";

withDefaults(
  defineProps<{
    label: string;
    icon?: string;
    iconOnly?: boolean;
  }>(),
  {
    iconOnly: false,
  }
);
</script>

<template>
  <button
    type="button"
    class="flex items-center gap-2 text-menu transition-colors font-display text-heading-2xl hover:underline"
  >
    <BaseIcon v-if="icon" :name="icon" />
    <span v-if="iconOnly" class="sr-only">{{ label }}</span>
    <span v-else class="hidden transform translate-y-px md:block">
      {{ label }}
    </span>
  </button>
</template>
