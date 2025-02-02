<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>
<template>
  <div>
    <h1 class="text-heading-6xl text-favorite hover:text-favorite-hover">
      {{ title }}
    </h1>
    <p class="mt-2" v-if="description">{{ description }}</p>
    <slot></slot>
    <div class="mt-4">Source code:</div>
    <SourceCode class="mt-4" />
  </div>
</template>
