<template>
  <div v-html="banner"></div>
</template>

<script setup lang="ts">
import { useSettingsStore } from "../stores/settingsStore";
const settingsStore = useSettingsStore();
const banner = settingsStore.config.banner;
</script>
