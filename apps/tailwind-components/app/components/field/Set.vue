<template>
  <fieldset class="border border-gray-900 p-2 space-y-4">
    <legend class="m-2 px-2">{{ label }}</legend>
    <slot></slot>
  </fieldset>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
}>();
</script>
