<script lang="ts" setup>
import { ref } from "vue";

// @ts-expect-error
import { DashboardRow } from "molgenis-viz";
import ProviderDashboard from "./ProviderDashboard.vue";
import LevelGroupedColumnChart from "./LevelGroupedColumnChart.vue";
import { getDashboardPage } from "../utils/getDashboardData";

import type { IDashboardPages, ICharts, IChartData } from "../types/schema.js";
import type { IAppPage } from "../types/app";

interface LevelTemplateProps {
  name: string;
  props: IAppPage;
  enableFilter?: boolean;
  filterProperty?: string;
  filterTitle?: string;
}
const props = withDefaults(defineProps<LevelTemplateProps>(), {
  enableFilter: false,
});

const dashboardPage = ref<IDashboardPages>();
const ernPageData = ref<IDashboardPages>();

async function getPageData() {
  const currentProvider = await getDashboardPage(
    props.props.api.graphql.current,
    props.name
  );

  const ernData = await getDashboardPage(
    props.props.api.graphql.providers,
    props.name
  );

  dashboardPage.value = currentProvider[0];
  ernPageData.value = ernData[0];
}

getPageData();
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">{{ name }}</h2>
    <DashboardRow :columns="1">
      <template v-for="chart in dashboardPage?.charts">
        <LevelGroupedColumnChart
          :chart="chart"
          :enableFilter="enableFilter"
          :filterProperty="filterProperty"
          :filterTitle="filterTitle"
          :ernLevelData="(ernPageData?.charts?.filter(row=>row.chartId === chart.chartId)[0] as ICharts).dataPoints"
        />
      </template>
    </DashboardRow>
  </ProviderDashboard>
</template>
