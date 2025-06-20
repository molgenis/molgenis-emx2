<script setup lang="ts">
import { useCookie } from "#app";

const cookieTheme = useCookie("theme");
</script>

<template>
  <ThemeSwitch />
  <div class="mt-4 text-title">cookie theme: {{ cookieTheme }}</div>
</template>
