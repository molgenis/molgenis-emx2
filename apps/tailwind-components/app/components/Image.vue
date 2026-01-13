<script setup lang="ts">
import type { Img } from "~~/types/pages";

withDefaults(defineProps<Img>(), {
  isCentered: false,
});
</script>

<template>
  <img
    :src="src"
    :class="[
      {
        'text-center': isCentered,
      },
      width,
      height,
    ]"
    :alt="alt"
  />
</template>
