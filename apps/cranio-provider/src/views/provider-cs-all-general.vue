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
          @change="updateCranioTypesChart"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :yMax="cranioTypeChart?.yAxisMaxValue"
          :yTickValues="cranioTypeChart?.yAxisTicks"
          :xAxisLabel="cranioTypeChart?.xAxisLabel"
          :yAxisLAbel="cranioTypeChart?.yAxisLabel"
          :columnColorPalette="cranioTypeChartPalette"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :yMin="0"
          :yMax="affectedSutureChart?.yAxisMaxValue"
          :yTickValues="affectedSutureChart?.yAxisTicks"
          :xAxisLabel="affectedSutureChart?.xAxisLabel"
          :yAxisLabel="affectedSutureChart?.yAxisLabel"
          :columnColorPalette="affectedSutureChartPalette"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="multipleSutureChart?.xAxisLabel"
          :yAxisLabel="multipleSutureChart?.yAxisLabel"
          :yMin="0"
          :yMax="multipleSutureChart?.yAxisMaxValue"
          :yTickValues="multipleSutureChart?.yAxisTicks"
          :columnColorPalette="multipleSuturePalette"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="patientsByCountryChart?.xAxisLabel"
          :yAxisLabel="patientsByCountryChart?.yAxisLabel"
          :yMin="0"
          :yMax="patientsByCountryChart?.yAxisMaxValue"
          :columnColorPalette="patientsByCountryPalette"
          :chartHeight="275"
          :chartMargins="{
            top: patientsByCountryChart?.topMargin,
            right: patientsByCountryChart?.rightMargin,
            bottom: patientsByCountryChart?.bottomMargin,
            left: patientsByCountryChart?.leftMargin,
          }"
        />
        <!-- ["#4269d0","#efb118","#ff725c","#6cc5b0","#3ca951","#ff8ab7","#a463f2","#97bbf5","#9c6b4e","#9498a0"] -->
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  InputLabel,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../utils/generateAxisTicks";
import { getDashboardChart } from "../utils/getDashboardData";
import { generateColorPalette } from "../utils/generateColorPalette";
import { uniqueValues } from "../utils";

import type { ICharts, IChartData } from "../types/schema";
import type { IKeyValuePair } from "../types";
import type { IAppPage } from "../types/app";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const error = ref<string>();
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const cranioTypeChart = ref<ICharts>();
const cranioTypeChartData = ref<IChartData[]>();
const cranioTypeChartPalette = ref<string[]>();
const affectedSutureChart = ref<ICharts>();
const affectedSutureChartData = ref<IChartData[]>();
const affectedSutureChartPalette = ref<string[]>();
const multipleSutureChart = ref<ICharts>();
const multipleSutureChartData = ref<IChartData[]>();
const multipleSuturePalette = ref<string[]>();
const patientsByCountryChart = ref<ICharts>();
const patientsByCountryChartData = ref<IChartData[]>();
const patientsByCountryPalette = ref<string[]>();

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
    uniqueValues(cranioTypeChart.value?.dataPoints, "dataPointName")
  );

  affectedSutureChartPalette.value = generateColorPalette(
    uniqueValues(affectedSutureChart.value?.dataPoints, "dataPointName")
  );

  multipleSuturePalette.value = generateColorPalette(
    uniqueValues(multipleSutureChart.value?.dataPoints, "dataPointName")
  );

  patientsByCountryPalette.value = generateColorPalette(
    uniqueValues(patientsByCountryChart.value.dataPoints, "dataPointName")
  );
}

function updateCranioTypesChart() {
  cranioTypeChartData.value = cranioTypeChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    })
    .sort((current, next) => {
      return current.dataPointOrder! - next.dataPointOrder!;
    });

  const cranioTypeTicks = generateAxisTickData(
    cranioTypeChartData.value,
    "dataPointValue"
  );

  if (cranioTypeChart.value) {
    cranioTypeChart.value.yAxisMaxValue = cranioTypeTicks.limit;
    cranioTypeChart.value.yAxisTicks = cranioTypeTicks.ticks;
  }
}

function updateAffectedSutureChart() {
  affectedSutureChartData.value = affectedSutureChart.value?.dataPoints?.sort(
    (current, next) => {
      return current.dataPointOrder! - next.dataPointOrder!;
    }
  );

  const affectedSutureTicks = generateAxisTickData(
    affectedSutureChartData.value,
    "dataPointValue"
  );

  if (affectedSutureChart.value) {
    affectedSutureChart.value.yAxisMaxValue = affectedSutureTicks.limit;
    affectedSutureChart.value.yAxisTicks = affectedSutureTicks.ticks;
  }
}

function updateMultipeSuturesChart() {
  multipleSutureChartData.value = multipleSutureChart.value?.dataPoints?.sort(
    (current, next) => {
      return current.dataPointOrder! - next.dataPointOrder!;
    }
  );

  const multipleSutureTicks = generateAxisTickData(
    multipleSutureChartData.value,
    "dataPointValue"
  );
  if (multipleSutureChart.value) {
    multipleSutureChart.value.yAxisMaxValue = multipleSutureTicks.limit;
    multipleSutureChart.value.yAxisTicks = multipleSutureTicks.ticks;
  }
}

function updatePatientsByCountryChart() {
  patientsByCountryChartData.value =
    patientsByCountryChart.value?.dataPoints?.sort((current, next) => {
      return current.dataPointName?.localeCompare(
        next.dataPointName as string
      ) as number;
    });

  const countries: string[] = uniqueValues(
    patientsByCountryChartData.value,
    "dataPointName"
  );
  const colors = countries.map((key: string) => [key, "#3f6597"]);
  patientsByCountryPalette.value = Object.fromEntries(colors);
}

function setAgeGroupFilter() {
  ageGroups.value = uniqueValues(
    cranioTypeChart.value?.dataPoints,
    "dataPointPrimaryCategory"
  );
  selectedAgeGroup.value = ageGroups.value[0];
}

onBeforeMount(async () => {
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
