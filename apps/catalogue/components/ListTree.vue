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
    <h3 class="mb-2.5 font-bold">{{ title }}</h3>
    <ul class="list-outside list-disc pl-4 grid grid-cols-3">
      <slot></slot>
    </ul>
  </section>
</template>
