<script setup lang="ts">
import type { Header } from "../../../types/pages";
import Heading from "../text/Heading.vue";

const props = withDefaults(defineProps<Header>(), {
  enableFullScreenWidth: true,
});
</script>

<template>
  <header
    class="relative flex justify-start items-center bg-cover bg-center h-72"
    :class="{
      'text-gray-100': backgroundImage,
      'text-title bg-gray-100': !backgroundImage,
    }"
    :style="backgroundImage ? `background-image: url(${backgroundImage})` : ''"
  >
    <div class="w-page-sections mx-8 z-10">
      <h1 class="font-display text-heading-4xl">{{ title }}</h1>
      <p class="text-heading-2xl">{{ subtitle }}</p>
    </div>
    <div
      v-if="backgroundImage"
      class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    />
  </header>
</template>
