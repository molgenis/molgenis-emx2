<script setup lang="ts">
const modelValue = defineModel<string | boolean>();
</script>

<template>
  <input v-model="modelValue" type="checkbox" class="hover:cursor-pointer" />
</template>
