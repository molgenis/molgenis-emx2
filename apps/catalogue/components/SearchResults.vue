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

  <div class="pb-12.5">
    <slot name="pagination"></slot>
  </div>
</template>
