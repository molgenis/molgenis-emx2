<script setup lang="ts">
const modelValue = defineModel<boolean | string>();
</script>

<template>
  <input v-model="modelValue" type="checkbox" class="hover:cursor-pointer" />
</template>
