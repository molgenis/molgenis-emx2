<script setup lang="ts">
withDefaults(defineProps<{ isCentered?: boolean }>(), { isCentered: false });
</script>

<template>
  <p :class="{ 'text-center': isCentered }"><slot /></p>
</template>
