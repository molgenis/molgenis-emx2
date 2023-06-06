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
  maxWidth: {
    type: String,
  },
  type: {
    default: "standard",
    enum: ["standard", "link"],
  },
});

const LIST_TYPE_MAPPING = {
  standard: "list-outside list-disc ml-4",
  link: "",
};

const COLUMNCOUNT = {
  1: "md:columns-1",
  2: "md:columns-2",
  3: "md:columns-3",
  4: "md:columns-4",
};

const listClasses = computed(() => {
  return LIST_TYPE_MAPPING[props.type];
});

const columnCountClass = computed(() => {
  return COLUMNCOUNT[props.columnCount];
});
</script>

<template>
  <section>
    <h3 class="mb-2.5 font-bold text-body-base">{{ title }}</h3>
    <ul
      class="columns-1 float-left text-body-base"
      :class="`${columnCountClass} ${listClasses} `">
      <slot></slot>
    </ul>
  </section>
</template>
