<script lang="ts" setup>
import { ref, computed } from "vue";

// @ts-expect-error
import { DashboardRow, MessageBox } from "molgenis-viz";
import ProviderDashboard from "./ProviderDashboard.vue";
import LevelGroupedColumnChart from "./LevelGroupedColumnChart.vue";
import { getDashboardPage } from "../../../metadata-utils/src/viz/getUiDashboardPage.js";

import type {
  IDashboardPages,
  ICharts,
} from "../../../metadata-utils/src/viz/UiDashboard";
import type { IAppPage } from "../types";
import type { ISiteErnCleftTypeCounts } from "../types/index.js";

interface LevelTemplateProps {
  name: string;
  props: IAppPage;
  enableFilter?: boolean;
  filterProperty?: string;
  filterTitle?: string;
  chartDescription?: string;
  numberOfPatientsByCleftType?: ISiteErnCleftTypeCounts;
}
const props = withDefaults(defineProps<LevelTemplateProps>(), {
  enableFilter: false,
});

const error = ref<string>();
const dashboardPage = ref<IDashboardPages>();
const ernPageData = ref<IDashboardPages>();

const pageTitle = computed<string>(() => {
  return dashboardPage.value?.description || props.name;
});

async function getPageData() {
  const currentProvider = await getDashboardPage(
    props.props.api.graphql.current,
    props.name
  );

  const ernData = await getDashboardPage(
    props.props.api.graphql.providers,
    props.name
  );

  dashboardPage.value = currentProvider[0] as IDashboardPages;
  ernPageData.value = ernData[0] as IDashboardPages;
}

getPageData().catch((err) => (error.value = err));
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">{{ pageTitle }}</h2>
    <MessageBox type="error" v-if="error">
      {{ error }}
    </MessageBox>
    <DashboardRow :columns="1">
      <template v-for="chart in dashboardPage?.charts">
        <LevelGroupedColumnChart
          :chart="chart"
          :enableFilter="enableFilter"
          :filterProperty="filterProperty"
          :filterTitle="filterTitle"
          :ernLevelData="(ernPageData?.charts?.filter(row=>row.chartId === chart.chartId)[0] as ICharts).dataPoints"
          :numberOfPatientsByCleftType="
            numberOfPatientsByCleftType || undefined
          "
          :chartDescription="chartDescription"
        />
      </template>
    </DashboardRow>
  </ProviderDashboard>
</template>
