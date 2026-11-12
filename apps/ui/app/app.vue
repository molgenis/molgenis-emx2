<script lang="ts" setup>
import { useHead } from "#app";
import { useApplyConfiguredTheme } from "./composables/useThemeSettings";

useHead({
  htmlAttrs: {
    lang: "en-GB",
  },
});
useApplyConfiguredTheme();
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
