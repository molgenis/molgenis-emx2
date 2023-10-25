<script setup>
const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  width: {
    type: Number,
    default: 24,
  },
});

function clearAndUpper(text) {
  return text.replace(/-/, "").toUpperCase();
}

function toPascalCase(text) {
  return text.replace(/(^\w|-\w)/g, clearAndUpper);
}

const componentName = computed(() => "Icons" + toPascalCase(props.name));
</script>

<template>
  <component :is="componentName" :width="width"></component>
</template>
