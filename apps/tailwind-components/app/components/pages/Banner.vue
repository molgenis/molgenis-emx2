<script setup lang="ts">
import type { IHeaders } from "../../../types/cms";

withDefaults(defineProps<IHeaders>(), {
  enableFullScreenWidth: false,
});
</script>

<template>
  <header
    :id="id"
    class="relative flex justify-center items-center h-80"
    :class="{
      'text-gray-100 bg-cover bg-center': backgroundImage,
      'text-title': !backgroundImage,
    }"
    :style="backgroundImage ? `background-image: url(${backgroundImage})` : ''"
  >
    <div
      class="m-auto mx-12.5 z-10"
      :class="{
        'w-pg-section': !enableFullScreenWidth,
        'w-full': enableFullScreenWidth,
        'text-center': titleIsCentered,
      }"
    >
      <h1 class="font-display text-heading-6xl">{{ title }}</h1>
      <p class="text-body-lg">{{ subtitle }}</p>
    </div>
    <div
      v-if="backgroundImage"
      class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    />
  </header>
</template>
