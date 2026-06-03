<script lang="ts" setup>
import { ref } from "vue";

// @ts-expect-error
import { DashboardRow } from "molgenis-viz";
import ProviderDashboard from "./ProviderDashboard.vue";
import LevelGroupedColumnChart from "./LevelGroupedColumnChart.vue";
import { getDashboardPage } from "../utils/getDashboardData";

import type { IDashboardPages } from "../types/schema.js";
import type { IAppPage } from "../types/app";

const loading = ref<boolean>(true);
const props = defineProps<IAppPage & { pageName: string }>();

const dashboardPage = ref<IDashboardPages>();

// TODO: fix this later
dashboardPage.value = await getDashboardPage(
  props.api.graphql.current,
  props.pageName
);
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">{{ pageName }}</h2>
    <DashboardRow :columns="1">
      <template v-for="chart in dashboardPage?.charts">
        <LevelGroupedColumnChart :chart="chart" />
      </template>
    </DashboardRow>
  </ProviderDashboard>
</template>
