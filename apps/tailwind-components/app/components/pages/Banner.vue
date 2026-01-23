<script setup lang="ts">
import type { IHeaders } from "../../../types/cms";

withDefaults(defineProps<IHeaders>(), {
  enableFullScreenWidth: true,
});
</script>

<template>
  <header
    class="relative flex justify-start items-center h-72"
    :class="{
      'text-gray-100 bg-cover bg-center': backgroundImage,
      'text-title': !backgroundImage,
    }"
    :style="backgroundImage ? `background-image: url(${backgroundImage})` : ''"
  >
    <div class="w-page-sections mx-7.5 z-10">
      <h1 class="font-display text-heading-6xl">{{ title }}</h1>
      <p class="text-body-lg">{{ subtitle }}</p>
    </div>
    <div
      v-if="backgroundImage"
      class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    />
  </header>
</template>
