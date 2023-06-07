<script setup lang="ts">
defineProps<{
  icon: string;
}>();
</script>
<template>
  <div
    class="flex flex-col items-start text-title max-w-xs px-5 pt-5 pb-3 antialiased lg:pb-10 lg:px-0">
    <span class="mb-2 mt-2.5 block text-icon">
      <BaseIcon :name="icon" :width="32" class="text-title" />
    </span>
    <p class="mt-1 mb-0 lg:mb-5 text-body-lg">
      <slot></slot>
    </p>
  </div>
</template>
