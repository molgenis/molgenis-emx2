<script lang="ts" setup>
import { useHead } from "#app";
import { computed } from "vue";
import { useRoute } from "vue-router";

type LayoutName = "default" | "playground" | "wide";

const route = useRoute();
const layoutName = computed<LayoutName>(() => {
  if (route.path === "/layouts/wideLayout") {
    return "wide";
  }

  if (route.path === "/layouts/defaultLayout") {
    return "default";
  }

  return "playground";
});

useHead({
  htmlAttrs: {
    lang: "en-GB",
  },
});
</script>

<template>
  <NuxtLayout :name="layoutName">
    <NuxtPage />
  </NuxtLayout>
</template>
