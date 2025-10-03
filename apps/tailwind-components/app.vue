<script lang="ts" setup>
import { useHead } from "#app";
useHead({
  htmlAttrs: {
    lang: "en-GB",
  },
});
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
