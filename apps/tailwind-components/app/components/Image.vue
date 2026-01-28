<script setup lang="ts">
import type { IImages } from "../../types/cms";

withDefaults(defineProps<IImages & { src: string }>(), {
  imageIsCentered: false,
});
</script>

<template>
  <img
    :src="image?.url || src"
    :class="[
      {
        'text-center': imageIsCentered,
      },
      width,
      height,
    ]"
    :alt="alt"
  />
</template>
