<script setup lang="ts">
import { ref, computed } from "vue";
import LevelTemplate from "../../components/LevelTemplate.vue";

import { getDashboardChart } from "../../../../metadata-utils/src/viz/getUiDashboardCharts";
import type { IChartData } from "../../../../metadata-utils/src/viz/UiDashboard.js";
import type { IAppPage } from "../../types";

const props = defineProps<IAppPage>();
const siteCfm = ref<IChartData>();
const ernCfm = ref<IChartData>();

const chartDescription = computed<string>(() => {
  const description: string[] = ["Number of Facial Dysostosis patients"];
  if (siteCfm.value) {
    description.push(`from your site (n=${siteCfm.value.value})`);
  }

  if (ernCfm.value) {
    description.push(`and in the ERN (n=${ernCfm.value.value})`);
  }
  return description.join(" ");
});

async function getData() {
  const sitePatientCounts = await getDashboardChart(
    props.api.graphql.current,
    "patients-by-workstream"
  );

  const ernPatientCounts = await getDashboardChart(
    props.api.graphql.providers,
    "patients-by-workstream"
  );

  siteCfm.value = sitePatientCounts[0].dataPoints?.filter((row: IChartData) => {
    return row.name === "Facial Dysostosis Syndromes";
  })[0];

  ernCfm.value = ernPatientCounts[0].dataPoints?.filter((row: IChartData) => {
    return row.name === "Facial Dysostosis Syndromes";
  })[0];
}

getData();
</script>

<template>
  <LevelTemplate
    name="Facial dysostosis level 1"
    :props="props"
    :chartDescription="chartDescription"
  />
</template>
