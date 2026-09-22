<script setup>
defineProps({
  title: {
    type: String,
  },
  count: {
    type: Number,
  },
  tooltip: {
    type: String, // yield component?? would be nice
  },
});
</script>

<template>
  <li>
    {{ title }}
    {{ count }}
    {{ tooltip }}
    <slot></slot>
  </li>
</template>
