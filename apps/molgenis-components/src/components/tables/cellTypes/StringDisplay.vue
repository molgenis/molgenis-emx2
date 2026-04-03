<template>
  <div>{{ data }}</div>
</template>

<script setup lang="ts">
const { data } = defineProps<{
  data: string | Record<string, any> | number | boolean;
}>();
</script>
