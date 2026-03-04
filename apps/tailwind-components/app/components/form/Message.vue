<template>
  <div class="flex flex-row bg-valid p-2 justify-between min-h-[64px]">
    <div class="flex flex-row gap-1">
      <BaseIcon name="info" class="text-valid stroke-2 min-h-6 min-w-6" />
      <span class="my-auto text-valid font-bold px-1">{{ message }}</span>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import BaseIcon from "../BaseIcon.vue";
defineProps<{
  message: string;
}>();
</script>
