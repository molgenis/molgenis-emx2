<script setup lang="ts">
const modelValue = defineModel<string | boolean>();
</script>

<template>
  <input
    v-model="modelValue"
    type="checkbox"
    class="w-5 h-5 hover:cursor-pointer accent-theme"
  />
</template>
