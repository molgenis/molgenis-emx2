<script setup lang="ts">
import LevelTemplate from "../../components/LevelTemplate.vue";
import type { IAppPage } from "../../types/app";
const props = defineProps<IAppPage>();
</script>

<template>
  <LevelTemplate name="Facial dysostosis level 1" :props="props" />
</template>
