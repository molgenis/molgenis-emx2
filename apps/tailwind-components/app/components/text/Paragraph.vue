<script setup lang="ts">
import type { IParagraphs } from "../../../types/cms";

withDefaults(defineProps<IParagraphs>(), {
  isCentered: false,
});
</script>

<template>
  <p class="text-title-contrast" :class="{ 'text-center': isCentered }">
    <slot />
  </p>
</template>
