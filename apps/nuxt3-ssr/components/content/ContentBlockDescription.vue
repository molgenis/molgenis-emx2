<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<template>
  <ContentBlock :title="title" :description="description" />
</template>
