<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for your center</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel id="yearOfBirthFilter" label="Filter data by age group" />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          @change="filterChartDatasets"
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
          :chartHeight="225"
          :chartMargins="{
            top: cranioTypeChart?.topMargin,
            right: cranioTypeChart?.rightMargin,
            bottom: cranioTypeChart?.bottomMargin,
            left: cranioTypeChart?.leftMargin,
          }"
        />
        <!-- :columnColorPalette="" -->
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Suture Overview</h3>
    <p class="dashboard-text">
      Click a category in the "Affected suture" chart to view more information.
    </p>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" />
        <ColumnChart
          v-else
          :chartId="affectedSutureChart?.chartId"
          :title="affectedSutureChart?.chartTitle"
          :description="affectedSutureChart?.chartSubtitle"
          class="chart-axis-x-angled-text"
          :chartData="affectedSutureChartData"
          xvar="dataPointName"
          yvar="dataPointValue"
          :yMax="affectedSutureChart?.yAxisMaxValue"
          :yTickValues="affectedSutureChart?.yAxisTicks"
          :chartHeight="275"
          :enableClicks="true"
          :chartMargins="{
            top: affectedSutureChart?.topMargin,
            right: affectedSutureChart?.rightMargin,
            bottom: affectedSutureChart?.bottomMargin,
            left: affectedSutureChart?.leftMargin,
          }"
        />
        <!-- @column-clicked="updateSutureTypes" -->
        <!-- :columnColorPalette="colors.affectedSuture" -->
      </DashboardChart>
      <!-- <DashboardChart v-if="showSutureTypes">
        <ColumnChart
          chartId="suture-types"
          class="chart-axis-x-angled-text"
          title="Multiple suture synostosis"
          :chartData="sutureTypes"
          xvar="type"
          yvar="count"
          :columnColorPalette="colors.sutureType"
          :chartHeight="275"
          :chartMargins="{ top: 20, right: 10, bottom: 85, left: 60 }"
        />
      </DashboardChart> -->
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
import { uniqueValues } from "../utils";

import type { ICharts, IChartData } from "../interfaces/schema";
import type { IAppPage } from "../interfaces/app";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const error = ref<string>();
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const cranioTypeChart = ref<ICharts>();
const cranioTypeChartData = ref<IChartData[]>();
const affectedSutureChart = ref<ICharts>();
const affectedSutureChartData = ref<IChartData[]>();
const multipleSutureChart = ref<ICharts>();
const multipleSutureChartData = ref<IChartData[]>();

async function getPageData() {
  const url: string = `/${props.organisation.schemaName}/api/graphql`;

  const csTypes = await getDashboardChart(
    url,
    "cs-provider-type-of-craniosynostosis"
  );
  cranioTypeChart.value = csTypes[0];

  const affectedSutures = await getDashboardChart(
    url,
    "cs-provider-affected-suture"
  );
  affectedSutureChart.value = affectedSutures[0];

  const multiSutures = await getDashboardChart(
    url,
    "cs-provider-multiple-suture-synostosis"
  );
  multipleSutureChart.value = multiSutures[0];
}

async function filterChartDatasets() {
  cranioTypeChartData.value = cranioTypeChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    }
  );

  affectedSutureChartData.value = affectedSutureChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    }
  );

  multipleSutureChartData.value = multipleSutureChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    }
  );
}

function setFilter() {
  ageGroups.value = uniqueValues(
    cranioTypeChart.value?.dataPoints,
    "dataPointPrimaryCategory"
  );
  selectedAgeGroup.value = ageGroups.value[0];
}

function setAxisTicks() {
  const cranioTypeTicks = generateAxisTickData(
    cranioTypeChartData.value,
    "dataPointValue"
  );

  const affectedSutureTicks = generateAxisTickData(
    affectedSutureChartData.value,
    "dataPointValue"
  );

  // @ts-expect-error
  cranioTypeChart.value.yAxisMaxValue = cranioTypeTicks.limit;
  // @ts-expect-error
  cranioTypeChart.value.yAxisTicks = cranioTypeTicks.ticks;
  // @ts-expect-error
  affectedSutureChart.value.yAxisMaxValue = affectedSutureTicks.limit;
  // @ts-expect-error
  affectedSutureChart.value.yAxisTicks = affectedSutureTicks.ticks;
}

onBeforeMount(async () => {
  try {
    await getPageData();
    setFilter();
    filterChartDatasets();
    setAxisTicks();
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
