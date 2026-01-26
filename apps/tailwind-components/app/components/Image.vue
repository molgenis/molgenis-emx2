<script setup lang="ts">
import type { IImages } from "../../types/cms";

withDefaults(defineProps<IImages & { src: string }>(), {
  isCentered: false,
});
</script>

<template>
  <img
    :src="image?.url || src"
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
