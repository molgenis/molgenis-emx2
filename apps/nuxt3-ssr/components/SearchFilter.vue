<script setup>
defineProps({
  title: {
    type: String,
  },
  expanded: {
    type: Boolean,
  },
});
</script>

<template>
  <h2>{{ title }}</h2>

  <slot>search-filter</slot>
</template>
