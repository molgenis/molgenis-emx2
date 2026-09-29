<script setup lang="ts">
import type { IHeadings } from "../../../../types/cms";
defineProps<IHeadings>();
</script>

<template>
  <component
    :is="`h${level}`"
    :id="id"
    class="text-title mb-2.5"
    :class="{
      'text-heading-6xl': level === 1,
      'text-heading-5xl': level === 2,
      'text-heading-4xl': level === 3,
      'text-heading-3xl': level === 4,
      'text-heading-2xl': level === 5,
      'text-heading-xl': level === 6,
      'w-full flex justify-center text-center': headingIsCentered,
      'sr-only': headingIsHidden,
    }"
  >
    {{ text }}
  </component>
</template>
