<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import { computed } from "vue";

const props = defineProps({
  title: {
    type: String,
  },
  columnCount: {
    type: Number,
  },
});

const COLUMNCOUNT = {
  1: "md:grid-cols-1",
  2: "md:grid-cols-2",
  3: "md:grid-cols-3",
  4: "md:grid-cols-4",
};

const columnCountClass = computed(() => {
  return COLUMNCOUNT[props.columnCount];
});
</script>

<template>
  <section>
    <h3 class="mb-2.5 font-bold">{{ title }}</h3>
    <div class="grid gap-6" :class="columnCountClass">
      <slot></slot>
    </div>
  </section>
</template>
