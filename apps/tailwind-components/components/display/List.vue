<template>
  <section>
    <h3 v-if="title" class="mb-2.5 font-bold text-body-base">{{ title }}</h3>
    <ul
      class="columns-1 text-body-base"
      :class="{
        'list-outside list-disc ml-4': props.type === 'standard',
        'md:columns-1': props.columnCount === 1,
        'md:columns-2': props.columnCount === 2,
        'md:columns-3': props.columnCount === 3,
        'md:columns-4': props.columnCount === 4,
      }"
    >
      <slot></slot>
    </ul>
  </section>
</template>

<script setup lang="ts">
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
</script>
