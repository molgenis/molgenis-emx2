<script setup lang="ts">
import type { Header } from "../../../types/pages";

const props = withDefaults(defineProps<Header>(), {
  enableFullScreenWidth: true,
});
</script>

<template>
  <header
    class="relative flex justify-start items-center bg-cover bg-center text-title h-60"
    :style="backgroundImage ? `background-image: url(${backgroundImage})` : ''"
  >
    <div class="w-page-sections mx-8 z-10">
      <h1 class="font-display text-heading-3xl">{{ title }}</h1>
      <p class="text-heading-5xl">{{ subtitle }}</p>
    </div>
    <div
      v-if="backgroundImage"
      class="absolute top-0 left-0 w-full h-full bg-blue-500 bg-opacity-50"
    />
  </header>
</template>
