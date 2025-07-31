<script lang="ts" setup>
withDefaults(
  defineProps<{
    label?: string;
  }>(),
  {
    label: "No results found",
  }
);
</script>

<template>
  <span class="text-input italic">{{ label }}</span>
</template>
