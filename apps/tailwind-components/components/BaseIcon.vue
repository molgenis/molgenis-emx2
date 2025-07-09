<template>
  <component :is="componentName" :width="width"></component>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    name: string;
    width?: number;
  }>(),
  {
    width: 24,
  }
);

function clearAndUpper(text: string): string {
  return text.replace(/-/, "").toUpperCase();
}

function toPascalCase(text: string): string {
  return text.replace(/(^\w|-\w)/g, clearAndUpper);
}

const componentName = computed<string>(() => toPascalCase(props.name));
</script>
