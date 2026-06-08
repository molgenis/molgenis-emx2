<script lang="ts" setup>
import { ref } from "vue";

// @ts-expect-error
import { DashboardRow } from "molgenis-viz";
import ProviderDashboard from "./ProviderDashboard.vue";
import LevelGroupedColumnChart from "./LevelGroupedColumnChart.vue";
import { getDashboardPage } from "../utils/getDashboardData";

import type { IDashboardPages } from "../types/schema.js";
import type { IAppPage } from "../types/app";

interface LevelTemplateProps {
  name: string;
  props: IAppPage;
  enableFilter?: boolean;
  filterProperty?: string;
}
const props = withDefaults(defineProps<LevelTemplateProps>(), {
  enableFilter: false,
});

const dashboardPage = ref<IDashboardPages>();

async function getPageData() {
  const page = await getDashboardPage(
    props.props.api.graphql.current,
    props.name
  );

  dashboardPage.value = page[0];
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
        />
      </template>
    </DashboardRow>
  </ProviderDashboard>
</template>
