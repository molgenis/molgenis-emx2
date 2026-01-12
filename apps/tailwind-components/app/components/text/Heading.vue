<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    headingLevel: number;
    isCentered?: boolean;
  }>(),
  {
    headingLevel: 2,
    isCentered: false,
  }
);

const h = `h${props.headingLevel}`;
</script>

<template>
  <component
    :is="h"
    :class="{
      'text-title text-heading-6xl': h === 'h1',
      'text-title text-heading-5xl': h === 'h2',
      'text-title text-heading-4xl': h === 'h3',
      'text-title text-heading-3xl': h === 'h4',
      'text-title text-heading-2xl': h === 'h5',
      'text-title text-heading-xl': h === 'h6',
      'text-center': isCentered,
    }"
  >
    <slot></slot>
  </component>
</template>
