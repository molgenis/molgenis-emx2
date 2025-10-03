<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    title?: string;
    columnCount?: number;
    type?: "standard" | "link";
  }>(),
  {
    columnCount: 1,
    type: "standard",
  }
);

const LIST_TYPE_MAPPING = {
  standard: "list-outside list-disc ml-4",
  link: "",
};

const COLUMNCOUNT: Record<number, string> = {
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
    <h3 v-if="title" class="mb-2.5 font-bold text-body-base">{{ title }}</h3>
    <ul
      class="columns-1 text-body-base"
      :class="`${columnCountClass} ${listClasses} `"
    >
      <slot></slot>
    </ul>
  </section>
</template>
