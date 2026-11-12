<template>
  <Error :error="error" />
</template>

<script setup lang="ts">
import { type NuxtError } from "nuxt/app";
import Error from "./pages/Error.vue";

const props = defineProps<{ error: NuxtError }>();
</script>
