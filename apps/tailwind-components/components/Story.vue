<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>
<template>
  <div class="px-12 py-4">
    <h1
      v-if="title"
      class="text-heading-6xl text-favorite hover:text-favorite-hover"
    >
      {{ title }}
    </h1>
    <p class="mt-2" v-if="description">{{ description }}</p>
    <slot></slot>
    <h2 class="text-heading-xl mt-4">Source code:</h2>
    <SourceCode :id="title" />
  </div>
</template>
