<script setup>
defineProps({
  title: {
    type: String,
  },
  columnCount: {
    type: Number,
  },
});
</script>

<template>
  <section>
    <h3 class="mb-2.5">{{ title }}</h3>
    <div class="grid grid-cols-2">
      <slot></slot>
    </div>
  </section>
</template>
