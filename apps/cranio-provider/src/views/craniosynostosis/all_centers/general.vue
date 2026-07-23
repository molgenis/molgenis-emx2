<script setup lang="ts">
import { ref, onMounted } from "vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  InputLabel,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";
import ProviderDashboard from "../../../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../../../../../tailwind-components/app/utils/viz.js";
import { getDashboardChart } from "../../../../../metadata-utils/src/viz/getUiDashboardCharts";
import { generateColorPalette } from "../../../utils/generateColorPalette";
import { uniqueValues, uniqueAgeGroups } from "../../../utils";
import {
  ernYourCenterPalette,
  columnHoverFillColor,
} from "../../../utils/variables";

import type {
  ICharts,
  IChartData,
} from "../../../../../metadata-utils/src/viz/UiDashboard";
import type { IAppPage, IKeyValuePair } from "../../../types";

const props = defineProps<IAppPage>();
const loading = ref<boolean>(true);
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const cranioTypeChart = ref<ICharts>();
const cranioTypeChartData = ref<IChartData[]>();
const cranioTypeChartPalette = ref<IKeyValuePair>();
const affectedSutureChart = ref<ICharts>();
const affectedSutureChartData = ref<IChartData[]>();
const affectedSutureChartPalette = ref<IKeyValuePair>();
const multipleSutureChart = ref<ICharts>();
const multipleSutureChartData = ref<IChartData[]>();
const multipleSuturePalette = ref<IKeyValuePair>();
const patientsByCountryChart = ref<ICharts>();
const patientsByCountryChartData = ref<IChartData[]>();
const patientsByCountryPalette = ref<IKeyValuePair>();

async function getPageData() {
  const csTypes = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-type-of-craniosynostosis"
  );

  const affectedSutures = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-affected-sutures"
  );

  const multiSutures = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-mutiple-suture-synostosis"
  );

  const patientCountry = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-patients-by-country"
  );

  cranioTypeChart.value = csTypes[0];
  affectedSutureChart.value = affectedSutures[0];
  multipleSutureChart.value = multiSutures[0];
  patientsByCountryChart.value = patientCountry[0];

  cranioTypeChartPalette.value = generateColorPalette(
    uniqueValues(cranioTypeChart.value?.dataPoints, "name")
  );

  affectedSutureChartPalette.value = generateColorPalette(
    uniqueValues(affectedSutureChart.value?.dataPoints, "name")
  );

  multipleSuturePalette.value = generateColorPalette(
    uniqueValues(multipleSutureChart.value?.dataPoints, "name")
  );

  patientsByCountryPalette.value = generateColorPalette(
    uniqueValues(patientsByCountryChart.value.dataPoints, "name")
  );
}

function updateCranioTypesChart() {
  cranioTypeChartData.value = cranioTypeChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.primaryCategory === selectedAgeGroup.value;
    })
    .sort((current: IChartData, next: IChartData) => {
      return (current.sortOrder as number) - (next.sortOrder as number);
    });

  const cranioTypeTicks = generateAxisTickData(
    cranioTypeChartData.value as IChartData[],
    "value"
  );

  if (cranioTypeChart.value) {
    cranioTypeChart.value.yAxisMaxValue = cranioTypeTicks.limit;
    cranioTypeChart.value.yAxisTicks = cranioTypeTicks.ticks;
  }
}

function updateAffectedSutureChart() {
  affectedSutureChartData.value = affectedSutureChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.primaryCategory === selectedAgeGroup.value;
    })
    .sort((current: IChartData, next: IChartData) => {
      return (current.sortOrder as number) - (next.sortOrder as number);
    });

  const affectedSutureTicks = generateAxisTickData(
    affectedSutureChartData.value as IChartData[],
    "value"
  );

  if (affectedSutureChart.value) {
    affectedSutureChart.value.yAxisMaxValue = affectedSutureTicks.limit;
    affectedSutureChart.value.yAxisTicks = affectedSutureTicks.ticks;
  }
}

function updateMultipeSuturesChart() {
  multipleSutureChartData.value = multipleSutureChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.primaryCategory === selectedAgeGroup.value;
    })
    .sort((current: IChartData, next: IChartData) => {
      return (current.sortOrder as number) - (next.sortOrder as number);
    });

  const multipleSutureTicks = generateAxisTickData(
    multipleSutureChartData.value!,
    "value"
  );
  if (multipleSutureChart.value) {
    multipleSutureChart.value.yAxisMaxValue = multipleSutureTicks.limit;
    multipleSutureChart.value.yAxisTicks = multipleSutureTicks.ticks;
  }
}

function updatePatientsByCountryChart() {
  patientsByCountryChartData.value =
    patientsByCountryChart.value?.dataPoints?.sort((current, next) => {
      return current.name?.localeCompare(next.name as string) as number;
    });

  const countryTicks = generateAxisTickData(
    patientsByCountryChartData.value!,
    "value"
  );
  if (patientsByCountryChart.value) {
    patientsByCountryChart.value.yAxisMaxValue = countryTicks.limit;
    patientsByCountryChart.value.yAxisTicks = countryTicks.ticks;
  }
}

function setAgeGroupFilter() {
  ageGroups.value = uniqueAgeGroups(
    cranioTypeChart.value?.dataPoints,
    "primaryCategory"
  );
  selectedAgeGroup.value = ageGroups.value[0];
}

function updateChartsByAgeGroup() {
  updateCranioTypesChart();
  updateAffectedSutureChart();
  updateMultipeSuturesChart();
}

onMounted(() => {
  getPageData()
    .then(() => {
      setAgeGroupFilter();
      updateCranioTypesChart();
      updateAffectedSutureChart();
      updateMultipeSuturesChart();
      updatePatientsByCountryChart();
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => (loading.value = false));
});
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for all centers</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel
          id="yearOfBirthFilter"
          label="Filter data by year of birth"
        />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          v-model="selectedAgeGroup"
          @change="updateChartsByAgeGroup"
        >
          <option v-for="ageGroup in ageGroups" :value="ageGroup">
            {{ ageGroup }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart id="type-of-craniosynostosis">
        <LoadingScreen v-if="loading" />
        <ColumnChart
          v-else
          :chartId="cranioTypeChart?.chartId"
          :title="cranioTypeChart?.chartTitle"
          :description="cranioTypeChart?.chartSubtitle"
          :chartData="cranioTypeChartData"
          xvar="name"
          yvar="value"
          :yMax="cranioTypeChart?.yAxisMaxValue"
          :yTickValues="cranioTypeChart?.yAxisTicks"
          :xAxisLabel="cranioTypeChart?.xAxisLabel"
          :yAxisLAbel="cranioTypeChart?.yAxisLabel"
          :columnFill="ernYourCenterPalette['ERN']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="225"
          :chartMargins="{
            top: cranioTypeChart?.topMargin,
            right: cranioTypeChart?.rightMargin,
            bottom: cranioTypeChart?.bottomMargin,
            left: cranioTypeChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Suture Overview</h3>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" />
        <ColumnChart
          v-else
          class="chart-axis-x-angled-text"
          :chartId="affectedSutureChart?.chartId"
          :title="affectedSutureChart?.chartTitle"
          :description="affectedSutureChart?.chartSubtitle"
          :chartData="affectedSutureChartData"
          xvar="name"
          yvar="value"
          :yMin="0"
          :yMax="affectedSutureChart?.yAxisMaxValue"
          :yTickValues="affectedSutureChart?.yAxisTicks"
          :xAxisLabel="affectedSutureChart?.xAxisLabel"
          :yAxisLabel="affectedSutureChart?.yAxisLabel"
          :columnFill="ernYourCenterPalette['ERN']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="275"
          :chartMargins="{
            top: affectedSutureChart?.topMargin,
            right: affectedSutureChart?.rightMargin,
            bottom: affectedSutureChart?.bottomMargin,
            left: affectedSutureChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" />
        <ColumnChart
          v-else
          :chartId="multipleSutureChart?.chartId"
          class="chart-axis-x-angled-text"
          :title="multipleSutureChart?.chartTitle"
          :description="multipleSutureChart?.chartSubtitle"
          :chartData="multipleSutureChartData"
          xvar="name"
          yvar="value"
          :xAxisLabel="multipleSutureChart?.xAxisLabel"
          :yAxisLabel="multipleSutureChart?.yAxisLabel"
          :yMin="0"
          :yMax="multipleSutureChart?.yAxisMaxValue"
          :yTickValues="multipleSutureChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['ERN']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="275"
          :chartMargins="{
            top: multipleSutureChart?.topMargin,
            right: multipleSutureChart?.rightMargin,
            bottom: multipleSutureChart?.bottomMargin,
            left: multipleSutureChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Patients Overview</h3>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" />
        <ColumnChart
          v-else
          :chartId="patientsByCountryChart?.chartId"
          :title="patientsByCountryChart?.chartTitle"
          :description="patientsByCountryChart?.chartSubtitle"
          :chartData="patientsByCountryChartData"
          xvar="name"
          yvar="value"
          :xAxisLabel="patientsByCountryChart?.xAxisLabel"
          :yAxisLabel="patientsByCountryChart?.yAxisLabel"
          :yMin="0"
          :yMax="patientsByCountryChart?.yAxisMaxValue"
          :yTickValues="patientsByCountryChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['ERN']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="275"
          :chartMargins="{
            top: patientsByCountryChart?.topMargin,
            right: patientsByCountryChart?.rightMargin,
            bottom: patientsByCountryChart?.bottomMargin,
            left: patientsByCountryChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>
