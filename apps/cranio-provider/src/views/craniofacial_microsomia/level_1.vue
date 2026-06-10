<script setup lang="ts">
import LevelTemplate from "../../components/LevelTemplate.vue";
import type { IAppPage } from "../../types/app.js";
const props = defineProps<IAppPage>();
</script>

<template>
  <LevelTemplate name="Craniofacial microsomia level 1" :props="props" />
</template>
