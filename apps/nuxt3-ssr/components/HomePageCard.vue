<script setup lang="ts">
const props = defineProps<{
  title: string
  count: number
}>()
</script>

<template>
  <div class="text-center">
    <h3>{{ title }}</h3>
    <div>{{ count }}</div>
  </div>
</template>