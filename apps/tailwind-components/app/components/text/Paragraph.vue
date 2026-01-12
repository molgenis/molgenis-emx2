<script setup lang="ts">
withDefaults(defineProps<{ isCentered: boolean }>(), { isCentered: false });
</script>

<template>
  <p><slot /></p>
</template>
