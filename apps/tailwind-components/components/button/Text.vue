<script setup lang="ts">
defineProps<{
  icon?: string;
}>();
</script>

<template>
  <button class="flex items-center text-button-text">
    <BaseIcon v-if="icon" :name="icon" :width="18" />
    <span class="ml-2 text-body-sm hover:underline">
      <slot />
    </span>
  </button>
</template>
