<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>
<template>
  <h1>{{ title }}</h1>
  <p v-if="description">{{ description }}</p>
  <hr />
  <slot></slot>
</template>
