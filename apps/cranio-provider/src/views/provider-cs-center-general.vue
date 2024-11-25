<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for your center</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel id="yearOfBirthFilter" label="Filter data by age group" />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          @change="updateCharts"
        >
          <option v-for="ageGroup in ageGroups" :value="ageGroup">
            {{ ageGroup }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
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
          :columnColorPalette="affectedSutureChartPalette"
          :chartHeight="275"
          :enableClicks="true"
          :chartMargins="{
            top: affectedSutureChart?.topMargin,
            right: affectedSutureChart?.rightMargin,
            bottom: affectedSutureChart?.bottomMargin,
            left: affectedSutureChart?.leftMargin,
          }"
          @column-clicked="updateMultipeSuturesChart"
        />
      </DashboardChart>
      <DashboardChart v-if="selectedSutureType">
        <ColumnChart
          :chartId="multipleSutureChart?.chartId"
          class="chart-axis-x-angled-text"
          :title="`${selectedSutureType} ${multipleSutureChart?.chartTitle}`"
          :description="multipleSutureChart?.chartSubtitle"
          :chartData="multipleSutureChartData"
          xvar="dataPointName"
          yvar="dataPointValue"
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
  </ProviderDashboard>
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  InputLabel,
  LoadingScreen,
  //@ts-ignore
} from "molgenis-viz";
import { generateAxisTickData } from "../utils/generateAxisTicks";
import { getDashboardChart } from "../utils/getDashboardData";
import { generateColorPalette } from "../utils/generateColorPalette";
import { uniqueValues } from "../utils";

import type { ICharts, IChartData } from "../interfaces/schema";
import type { IAppPage } from "../interfaces/app";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const error = ref<string>();
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const selectedSutureType = ref<string>();
const cranioTypeChart = ref<ICharts>();
const cranioTypeChartData = ref<IChartData[]>();
const cranioTypeChartPalette = ref<string[]>();
const affectedSutureChart = ref<ICharts>();
const affectedSutureChartData = ref<IChartData[]>();
const affectedSutureChartPalette = ref<string[]>();
const multipleSutureChart = ref<ICharts>();
const multipleSutureChartData = ref<IChartData[]>();
const multipleSuturePalette = ref<string[]>();

async function getPageData() {
  const url: string = `/${props.organisation.schemaName}/api/graphql`;

  const csTypes = await getDashboardChart(
    url,
    "cs-provider-type-of-craniosynostosis"
  );
  cranioTypeChart.value = csTypes[0];
  const cranioTypeGroups: string[] = uniqueValues(
    cranioTypeChart.value.dataPoints, "dataPointName"
  )
  cranioTypeChartPalette.value = generateColorPalette(cranioTypeGroups);

  const affectedSutures = await getDashboardChart(
    url,
    "cs-provider-affected-suture"
  );
  affectedSutureChart.value = affectedSutures[0];
  
  const affectedSutureGroups: string[] = uniqueValues(affectedSutureChart.value.dataPoints, "dataPointName")
  affectedSutureChartPalette.value = generateColorPalette(affectedSutureGroups);

  const multiSutures = await getDashboardChart(
    url,
    "cs-provider-multiple-suture-synostosis"
  );
  multipleSutureChart.value = multiSutures[0];
  
  const mutlipleSutureGroups = uniqueValues(
    multipleSutureChart.value.dataPoints, "dataPointName"
  )
  multipleSuturePalette.value = generateColorPalette(mutlipleSutureGroups);
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
  affectedSutureChartData.value = affectedSutureChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    })
    .sort((current, next) => {
      return current.dataPointOrder! - next.dataPointOrder!;
    });

  const affectedSutureTicks = generateAxisTickData(
    affectedSutureChartData.value,
    "dataPointValue"
  );

  if (affectedSutureChart.value) {
    affectedSutureChart.value.yAxisMaxValue = affectedSutureTicks.limit;
    affectedSutureChart.value.yAxisTicks = affectedSutureTicks.ticks;
  }
}

function updateMultipeSuturesChart(value: string) {
  selectedSutureType.value = JSON.parse(value).dataPointSecondaryCategory;
  multipleSutureChartData.value = multipleSutureChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    })
    .sort((current, next) => {
      return current.dataPointOrder! - next.dataPointOrder!;
    });

  const multipleSutureTicks = generateAxisTickData(
    multipleSutureChartData.value,
    "dataPointValue"
  );
  if (multipleSutureChart.value) {
    multipleSutureChart.value.yAxisMaxValue = multipleSutureTicks.limit;
    multipleSutureChart.value.yAxisTicks = multipleSutureTicks.ticks;
  }
}

function setAgeGroupFilter() {
  ageGroups.value = uniqueValues(
    cranioTypeChart.value?.dataPoints,
    "dataPointPrimaryCategory"
  );
  selectedAgeGroup.value = ageGroups.value[0];
}

function updateCharts() {
  updateCranioTypesChart();
  updateAffectedSutureChart();
}

onBeforeMount(async () => {
  try {
    await getPageData();
    setAgeGroupFilter();
    updateCharts();
  } catch (err) {
    error.value = `${err}`;
  } finally {
    loading.value = false;
  }
});

// const props = defineProps({
//   user: String,
//   organization: Object,
// });

// define random data
// import { randomInt } from "d3";
// import generateColors from "../utils/palette.js";

// let csTotalCases = ref(0);
// let craniosynostosisTypes = ref([]);
// let affectedSuture = ref([]);
// let sutureTypes = ref([]);
// let showSutureTypes = ref(false);

// let colors = ref({
//   craniosynostosis: {},
//   affectedSuture: {},
//   sutureType: {},
// });

/// generate random data for craniosynostosis types
// function setCraniosynostosisTypes() {
//   const types = [
//     "Familial",
//     "Iatrogenic",
//     "Metabolic",
//     "Non-syndromic",
//     "Syndromic",
//   ];
//   colors.value.craniosynostosis = generateColors(types);
//   craniosynostosisTypes.value = types.map((type) => {
//     return { type: type, count: randomInt(10, 42)() };
//   });
// }

// // generate random data for affected suture
// function setAffectedSuture() {
//   const types = [
//     "Frontosphenoidal",
//     "Metopic",
//     "Multiple",
//     "Sagittal",
//     "Unicoronal",
//     "Unilambdoid",
//   ];
//   colors.value.affectedSuture = generateColors(types);
//   affectedSuture.value = types.map((category) => {
//     return { category: category, count: randomInt(10, 50)() };
//   });
// }

// function setSutureTypes() {
//   const types = [
//     "bicoronal",
//     "bilambdoid",
//     "bilambdoid+sagittal",
//     "pansynostosis",
//     "other",
//   ];
//   colors.value.sutureType = generateColors(types);
//   sutureTypes.value = types.map((type) => {
//     return { type: type, count: randomInt(3, 27)() };
//   });
// }

// // set update sutute type selection
// function updateSutureTypes(value) {
//   const total = JSON.parse(value).count;
//   const types = sutureTypes.value.map((entry) => entry.type);
//   let currentTotal = total;
//   sutureTypes.value = types.map((type, i) => {
//     const randomValue =
//       i === types.length - 1 ? currentTotal : randomInt(1, currentTotal)();
//     currentTotal -= randomValue;
//     return { type: type, count: randomValue };
//   });
//   console.log(sutureTypes.value);
//   showSutureTypes.value = true;
// }

// function onYearOfBirthFilter() {
//   setCraniosynostosisTypes();
//   setAffectedSuture();
//   setSutureTypes();
// }

// setCraniosynostosisTypes();
// setAffectedSuture();
// setSutureTypes();
</script>
