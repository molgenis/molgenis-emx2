<template>
  <Error :error="error" />
</template>

<script setup lang="ts">
import { type NuxtError } from "nuxt/app";
import Error from "./layouts/error.vue";

const props = defineProps({
  error: Object as () => NuxtError,
});
</script>
