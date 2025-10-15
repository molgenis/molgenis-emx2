<script setup lang="ts">
defineProps<{
  title?: string;
  description?: string;
}>();
</script>

<template>
  <div>
    <div v-if="title" class="flex items-start font-bold text-body-base">
      <div class="flex items-center gap-1">{{ title }}</div>
    </div>
    <div v-if="description" class="prose max-w-none mb-5">
      {{ description }}
    </div>
    <slot class="p-11"></slot>
  </div>
</template>
