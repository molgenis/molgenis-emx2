<script setup lang="ts">
import LevelTemplate from "../../components/LevelTemplate.vue";
import type { IAppPage } from "../../types/app";
const props = defineProps<IAppPage>();
</script>

<template>
  <LevelTemplate
    name="Cleft lip and palate level 1"
    :props="props"
    :enable-filter="true"
    filter-property="dataPointPrimaryCategory"
    filter-title="Filter by cleft type"
  />
</template>
