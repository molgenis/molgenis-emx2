<script setup>
defineProps({
  title: {
    type: String,
  },
  visible: {
    type: Boolean,
  },
});
</script>

<template>
  <h3>{{ title }}</h3>

  <slot>search-filter-group</slot>
</template>
