<script setup>
defineProps({
  title: {
    type: String,
  },
  icon: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <slot name="header">header</slot>

  <div>
    <slot name="search-results">search-results</slot>
  </div>

  <div>
    <slot name="pagination">pagination</slot>
  </div>
</template>
