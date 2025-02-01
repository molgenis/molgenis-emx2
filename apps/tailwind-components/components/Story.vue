<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>
<template>
  <h1 class="text-heading-6xl text-favorite hover:text-favorite-hover">{{ title }}</h1>
  <p class="mt-2" v-if="description">{{ description }}</p>
  <slot></slot>
</template>
