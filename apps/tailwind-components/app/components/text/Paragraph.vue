<script setup lang="ts">
import type { TextElements } from "../../../types/pages";

withDefaults(defineProps<TextElements>(), {
  isCentered: false,
});
</script>

<template>
  <p :class="{ 'text-center': isCentered }"><slot /></p>
</template>
