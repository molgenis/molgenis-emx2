<script setup lang="ts">
import type { IHeadings } from "../../../types/cms";

withDefaults(defineProps<IHeadings>(), {
  level: 2,
  headingIsCentered: false,
});
</script>

<template>
  <component
    :is="`h${level}`"
    class="text-title"
    :class="{
      'text-heading-6xl': level === 1,
      'text-heading-5xl': level === 2,
      'text-heading-4xl': level === 3,
      'text-heading-3xl': level === 4,
      'text-heading-2xl': level === 5,
      'text-heading-xl': level === 6,
      'text-center': headingIsCentered,
    }"
  >
    {{ text }}
    <slot></slot>
  </component>
</template>
