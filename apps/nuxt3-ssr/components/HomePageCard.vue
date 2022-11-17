<script setup lang="ts">
defineProps({
  title: String,
  count: Number,
});
</script>

<template>
  <div class="text-center">
    <h3>{{ title }}</h3>
    <div>{{ count }}</div>
  </div>
</template>