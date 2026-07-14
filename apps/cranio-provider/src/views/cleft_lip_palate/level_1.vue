<script setup lang="ts">
import { ref } from "vue";
import LevelTemplate from "../../components/LevelTemplate.vue";
import { getDashboardChart } from "../../../../metadata-utils/src/viz/getUiDashboardCharts";

import type {
  ICharts,
  IChartData,
} from "../../../../metadata-utils/src/viz/UiDashboard.js";
import type { IAppPage } from "../../types/app";
import type { ISiteErnCleftTypeCounts } from "../../types/index";

const props = defineProps<IAppPage>();
const patientsByCleftType = ref<ISiteErnCleftTypeCounts>({
  center: {
    CL: 0,
    CP: 0,
    CLA: 0,
    CLAP: 0,
  },
  ern: {
    CL: 0,
    CP: 0,
    CLA: 0,
    CLAP: 0,
  },
});

async function getData() {
  const centerCleftTypeCounts = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-patients-by-phenotype"
  );

  const ernCleftTypeCounts = await getDashboardChart(
    props.api.graphql.providers,
    "clp-all-patients-by-phenotype"
  );

  const centerCounts: ICharts = centerCleftTypeCounts[0];
  const ernCounts: ICharts = ernCleftTypeCounts[0];

  if (centerCounts.dataPoints) {
    const centerCountValues = centerCounts.dataPoints.map((row: IChartData) => {
      return [row.name, row.value];
    });

    patientsByCleftType.value.center = Object.fromEntries(centerCountValues);
  }

  if (ernCounts.dataPoints) {
    const ernCountValues = ernCounts.dataPoints.map((row: IChartData) => {
      return [row.name, row.value];
    });
    patientsByCleftType.value.ern = Object.fromEntries(ernCountValues);
  }
}

getData();
</script>

<template>
  <LevelTemplate
    name="Cleft lip and palate level 1"
    :props="props"
    :enable-filter="true"
    filter-property="primaryCategory"
    filter-title="Filter chart by cleft type"
    :number-of-patients-by-cleft-type="patientsByCleftType"
  />
</template>
