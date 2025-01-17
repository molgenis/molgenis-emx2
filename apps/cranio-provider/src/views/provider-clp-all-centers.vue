<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">
      Overview of patients {{ selectedAgeGroup }} of age
    </h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <h3>Options</h3>
        <InputLabel
          id="yearOfBirthFilter"
          label="Year of birth"
          description="Limit the results by year of birth"
        />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          v-model="selectedAgeGroup"
          @change="updateCharts"
        >
          <option v-for="age in ageGroups" :value="age">{{ age }}</option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart v-if="currentVisibleChart === 'cleftq'">
        <LoadingScreen v-if="loading" />
        <GroupedColumnChart
          v-else
          :chartId="cleftqOutcomesChart?.chartId"
          :title="cleftqOutcomesChart?.chartTitle"
          :description="cleftqOutcomesChart?.chartSubtitle"
          :chartData="cleftqOutcomesChartData"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="cleftqOutcomesChart?.xAxisLabel"
          :yAxisLabel="cleftqOutcomesChart?.yAxisLabel"
          :yMin="0"
          :yMax="cleftqOutcomesChart?.yAxisMaxValue"
          :yTickValues="cleftqOutcomesChart?.yAxisTicks"
          :columnColorPalette="chartColorPalette"
          :chartHeight="250"
          :chartMagins="{
            top: cleftqOutcomesChart?.topMargin,
            right: cleftqOutcomesChart?.rightMargin,
            bottom: cleftqOutcomesChart?.bottomMargin,
            left: cleftqOutcomesChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart v-else>
        <LoadingScreen v-if="loading" />
        <GroupedColumnChart
          v-else
          :chartId="icsOutcomesChart?.chartId"
          :title="icsOutcomesChart?.chartTitle"
          :description="icsOutcomesChart?.chartSubtitle"
          :chartData="icsOutcomesChartData"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxis="icsOutcomesChart?.xAxisLabel"
          :yAxis="icsOutcomesChart?.yAxisLabel"
          :yMin="0"
          :yMax="icsOutcomesChart?.yAxisMaxValue"
          :yTickValues="icsOutcomesChart?.yAxisTicks"
          :columnColorPalette="chartColorPalette"
          :chartHeight="250"
          :chartMagins="{
            top: icsOutcomesChart?.topMargin,
            right: icsOutcomesChart?.rightMargin,
            bottom: icsOutcomesChart?.bottomMargin,
            left: icsOutcomesChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import {
  DashboardRow,
  DashboardChart,
  GroupedColumnChart,
  InputLabel,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../utils/generateAxisTicks";
import { uniqueValues } from "../utils";
import { getDashboardChart } from "../utils/getDashboardData";

import type { ICharts, IChartData } from "../types/schema";
import type { IKeyValuePair } from "../types";
import type { IAppPage } from "../types/app";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const icsOutcomesChart = ref<ICharts>();
const icsOutcomesChartData = ref<IChartData[]>();
const cleftqOutcomesChart = ref<ICharts>();
const cleftqOutcomesChartData = ref<IChartData[]>();
const chartColorPalette = ref<IKeyValuePair>();

type chartTypes = "ics" | "cleftq";
const currentVisibleChart = ref<chartTypes>("ics");

async function getPageData() {
  const icsCenterResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-all-centers-ics-results"
  );

  const icsErnResponse = await getDashboardChart(
    props.api.graphql.providers,
    "clp-all-centers-ics-results"
  );

  const cleftqCenterResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-all-centers-cleft-q-outcomes"
  );

  const cleftqErnResponse = await getDashboardChart(
    props.api.graphql.providers,
    "clp-all-centers-cleft-q-outcomes"
  );

  icsOutcomesChart.value = icsCenterResponse[0];
  cleftqOutcomesChart.value = cleftqCenterResponse[0];
  icsOutcomesChart.value.dataPoints = [
    ...(icsCenterResponse[0].dataPoints as IChartData[]),
    ...(icsErnResponse[0].dataPoints as IChartData[]),
  ] as IChartData[];

  cleftqOutcomesChart.value.dataPoints = [
    ...(cleftqCenterResponse[0].dataPoints as IChartData[]),
    ...(cleftqErnResponse[0].dataPoints as IChartData[]),
  ];
}

function updateIcsChart() {
  icsOutcomesChartData.value = icsOutcomesChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    }
  );

  const chartTicks = generateAxisTickData(
    icsOutcomesChartData.value!,
    "dataPointValue"
  );

  if (icsOutcomesChart.value) {
    icsOutcomesChart.value.yAxisMaxValue = chartTicks.limit;
    icsOutcomesChart.value.yAxisTicks = chartTicks.ticks;
  }
}

function updateCleftqChart() {
  cleftqOutcomesChartData.value = cleftqOutcomesChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    }
  );
  const chartTicks = generateAxisTickData(
    cleftqOutcomesChartData.value!,
    "dataPointValue"
  );

  if (cleftqOutcomesChart.value) {
    cleftqOutcomesChart.value.yAxisMaxValue = chartTicks.limit;
    cleftqOutcomesChart.value.yAxisTicks = chartTicks.ticks;
  }
}

function updateCharts() {
  if (["3-4 years", "5-6 years"].includes(selectedAgeGroup.value as string)) {
    updateIcsChart();
    currentVisibleChart.value = "ics";
  } else {
    updateCleftqChart();
    currentVisibleChart.value = "cleftq";
  }
}

onMounted(() => {
  getPageData()
    .then(() => {
      if (icsOutcomesChart.value?.dataPoints) {
        icsOutcomesChart.value.dataPoints =
          icsOutcomesChart.value?.dataPoints.sort(
            (a: IChartData, b: IChartData) => {
              return a.dataPointName?.localeCompare(
                b.dataPointName as string
              ) as number;
            }
          );
      }

      if (cleftqOutcomesChart.value?.dataPoints) {
        cleftqOutcomesChart.value.dataPoints =
          cleftqOutcomesChart.value.dataPoints.sort(
            (a: IChartData, b: IChartData) => {
              return a.dataPointName?.localeCompare(
                b.dataPointName as string
              ) as number;
            }
          );
      }

      const distinctGroups = uniqueValues(
        [
          ...(cleftqOutcomesChart.value?.dataPoints as IChartData[]),
          ...(icsOutcomesChart.value?.dataPoints as IChartData[]),
        ],
        "dataPointSecondaryCategory"
      );

      if (distinctGroups.length > 2) {
        throw new Error(
          "For chart groupings, the number of groups cannot be more than 2."
        );
      } else {
        const colors = ["#9f6491", "#66c2a4"]; // purple, green
        chartColorPalette.value = Object.fromEntries(
          distinctGroups.map((value: string, index: number) => [
            value,
            colors[index],
          ])
        );
      }

      const distinctIcsAges = uniqueValues(
        icsOutcomesChart.value?.dataPoints,
        "dataPointPrimaryCategory"
      );
      const distinctCleftqAges = uniqueValues(
        cleftqOutcomesChart.value?.dataPoints,
        "dataPointPrimaryCategory"
      );
      const ageRanges = [...distinctCleftqAges, ...distinctIcsAges]
        .map((value: string) => {
          return {
            num: parseInt(value.split(/(-)/)[0] as string),
            label: value,
          };
        })
        .sort((a, b) => a.num - b.num);

      ageGroups.value = ageRanges.map((row) => row.label);
      selectedAgeGroup.value = ageGroups.value[0];
    })
    .then(() => {
      updateCharts();
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>
