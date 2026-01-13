<script setup lang="ts">
import type { HtmlHeadingLevels } from "../../../types/types";

const props = withDefaults(
  defineProps<{
    headingLevel: HtmlHeadingLevels;
    isCentered?: boolean;
  }>(),
  {
    headingLevel: "H2",
    isCentered: false,
  }
);
</script>

<template>
  <component
    :is="headingLevel"
    class="text-title"
    :class="{
      'text-heading-6xl': headingLevel === 'H1',
      'text-heading-5xl': headingLevel === 'H2',
      'text-heading-4xl': headingLevel === 'H3',
      'text-heading-3xl': headingLevel === 'H4',
      'text-heading-2xl': headingLevel === 'H5',
      'text-heading-xl': headingLevel === 'H6',
      'text-center': isCentered,
    }"
  >
    <slot></slot>
  </component>
</template>
