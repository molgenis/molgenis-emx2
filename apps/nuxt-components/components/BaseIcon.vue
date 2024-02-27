<script setup lang="ts">
import { computed, defineAsyncComponent } from "vue";

const props = withDefaults(
  defineProps<{
    name: string;
    width?: number;
  }>(),
  {
    name: "star-solid",
    width: 24,
  }
);

function clearAndUpper(text: string) {
  return text.replace(/-/, "").toUpperCase();
}

function toPascalCase(text: string) {
  return text.replace(/(^\w|-\w)/g, clearAndUpper);
}

const AsyncComp = computed(() => {
  const name = props.name || "star-solid";
  return defineAsyncComponent(
    () => import(`./icons/${toPascalCase(name)}.vue`)
  );
});
</script>

<template>
  <AsyncComp :width="width" />
</template>
