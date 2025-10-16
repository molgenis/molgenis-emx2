<template>
  <component :is="iconComp" :width="width"></component>
</template>

<script setup lang="ts">
import { withDefaults, defineProps, defineAsyncComponent } from "vue";

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

const componentName = toPascalCase(props.name);
const iconComp = defineAsyncComponent(
  () => import(`../components/global/icons/${componentName}.vue`)
);
</script>
