<script setup lang="ts">
defineProps<{
  icon?: string;
}>();
</script>

<template>
  <button
    class="group flex items-center text-button-text cursor-pointer disabled:cursor-not-allowed disabled:text-disabled"
  >
    <BaseIcon v-if="icon" :name="icon" :width="18" class="mr-2" />
    <span class="text-title-contrast group-enabled:hover:underline">
      <slot />
    </span>
  </button>
</template>
