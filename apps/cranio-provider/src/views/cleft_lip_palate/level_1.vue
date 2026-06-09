<script setup lang="ts">
import LevelTemplate from "../../components/LevelTemplate.vue";
import type { IAppPage } from "../../types/app";
const props = defineProps<IAppPage>();
</script>

<template>
  <LevelTemplate
    name="PROMs Charts"
    :props="props"
    :enable-filter="true"
    filter-property="dataPointPrimaryCategory"
    filter-title="Filter chart by cleft type"
  />
</template>
