<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
defineProps({
  title: {
    type: String,
  },
  columnCount: {
    type: Number,
  },
  maxWidth: {
    type: String,
  },
});
</script>

<template>
  <section>
    <h3 class="mb-2.5">{{ title }}</h3>
    <ul class="list-outside grid grid-cols-1 md:grid-cols-2 gap-2.5">
      <slot></slot>
    </ul>
  </section>
</template>
