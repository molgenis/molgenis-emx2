<script setup lang="ts">
import ContentClamp from "./ContentClamp.vue";

withDefaults(
  defineProps<{
    text?: string;
    /**
     * Lines shown before the text collapses. This replaced a character `cutoff`:
     * a character count cuts mid-word, says nothing about how much room the text
     * takes at this width, and left the remainder out of the markup entirely.
     */
    maxLines?: number;
  }>(),
  {
    maxLines: 5,
  }
);
</script>

<template>
  <div v-if="text" class="mb-5">
    <ContentClamp :maxLines="maxLines">{{ text }}</ContentClamp>
  </div>
</template>
