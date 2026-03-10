<script setup lang="ts">
import type { IParagraphs } from "../../../types/cms";

withDefaults(defineProps<IParagraphs>(), {
  paragraphIsCentered: false,
});
</script>

<template>
  <p
    class="text-title-contrast"
    :class="{ 'text-center': paragraphIsCentered }"
  >
    <slot />
  </p>
</template>
