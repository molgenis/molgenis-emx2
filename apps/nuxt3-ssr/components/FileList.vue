<script setup>
const props = defineProps({
  title: {
    type: String,
  },
  columnCount: {
    type: Number,
  },
});

function getColumnCountClass() {
  return `md:grid-cols-${props?.columnCount ? props?.columnCount : 1} `;
}
</script>

<template>
  <section>
    <h3 class="mb-2.5 font-bold text-body-base">{{ title }}</h3>
    <div class="grid gap-7.5 grid-cols-1 mb-2.5" :class="getColumnCountClass()">
      <slot></slot>
    </div>
  </section>
</template>
