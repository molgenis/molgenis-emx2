<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for all centers</h2>
    <h3 class="dashboard-h3">Overview of all surgical interventions</h3>
    <DashboardRow :columns="2" class="dashboard-boxes-width-2-1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="surgeryTypesChart?.chartId"
          :title="surgeryTypesChart?.chartTitle"
          :description="surgeryTypesChart?.chartSubtitle"
          :chartData="surgeryTypesChartData"
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="surgeryTypesChart?.xAxisLabel"
          :yAxisLabel="surgeryTypesChart?.yAxisLabel"
          xAxisLineBreaker=" "
          :yMin="0"
          :yMax="surgeryTypesChart?.yAxisMaxValue"
          :yTickValues="surgeryTypesChart?.yAxisTicks"
          :chartHeight="250"
          :chartMargins="{
            top: surgeryTypesChart?.topMargin,
            right: surgeryTypesChart?.rightMargin,
            bottom: surgeryTypesChart?.bottomMargin,
            left: surgeryTypesChart?.leftMargin,
          }"
          :enableClicks="true"
          @column-clicked="onSurgeryTypesChartClick"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <MessageBox
          v-else-if="!loading && !hasComplicationsData"
          type="warning"
        >
          <span>Not enough data to show chart</span>
        </MessageBox>
        <PieChart2
          v-else
          :chartId="complicationsChart?.chartId"
          :title="`${selectedSurgeryType?.split(' ')[0]} complications`"
          :description="complicationsChart?.chartSubtitle"
          :chartData="complicationsChartData"
          :chartColors="complicationsChartPalette"
          :valuesArePercents="false"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :enableClicks="true"
          :chartHeight="200"
          :chartScale="0.85"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <h2 class="dashboard-h2">Surgical interventions by diagnosis</h2>
      <DashboardChart>
        <InputLabel id="diagnosisInput" label="Select a diagnosis" />
        <select
          id="diagnosisInput"
          v-model="selectedDiagnosis"
          @change="
            updateInterventionsChart();
            updateSurgeryAgeChart();
          "
        >
          <option v-for="diagnosis in diagnoses" :value="diagnosis">
            {{ diagnosis }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 215px" />
        <MessageBox
          v-else-if="!loading && !hasInterventionsData"
          type="warning"
        >
          <span>Not enough data to show chart</span>
        </MessageBox>
        <PieChart2
          v-else
          :chartId="interventionsChart?.chartId"
          :title="interventionsChart?.chartTitle"
          :description="interventionsChart?.chartSubtitle"
          :chartData="interventionsChartData"
          :chartColors="{
            'First surgery': '#4e79a7',
            'Additional planned surgery according to protocol': '#f28e2c',
            'Unwanted reoperation due to complications': '#e15759',
          }"
          :valuesArePercents="false"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :stackLegend="true"
          :enableClicks="true"
          :chartHeight="200"
          :chartScale="0.85"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 215px" />
        <MessageBox v-else-if="!loading && !hasSurgeryAgeData" type="warning">
          <span>Not enough data to show chart</span>
        </MessageBox>
        <ColumnChart
          v-else
          :chartId="surgeryAgeChart?.chartId"
          :title="surgeryAgeChart?.chartTitle"
          :description="surgeryAgeChart?.chartSubtitle"
          :chartData="surgeryAgeChartData"
          xvar="dataPointTime"
          yvar="dataPointValue"
          :xAxisLabel="surgeryAgeChart?.xAxisLabel"
          :yAxisLabel="surgeryAgeChart?.yAxisLabel"
          :yMin="0"
          :yMax="surgeryAgeChart?.yAxisMaxValue"
          :yTickValues="surgeryAgeChart?.yAxisTicks"
          columnFill="#2a8f64"
          columnHoverFill="#ed7b23"
          :chartHeight="275"
          :chartMargins="{
            top: surgeryAgeChart?.topMargin,
            right: surgeryAgeChart?.rightMargin,
            bottom: surgeryAgeChart?.bottomMargin,
            left: surgeryAgeChart?.leftMargin,
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
  PieChart2,
  ColumnChart,
  InputLabel,
  LoadingScreen,
  MessageBox,
  // @ts-expect-error
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../utils/generateAxisTicks";
import { asKeyValuePairs, uniqueValues, sum, sumObjectValues } from "../utils";
import { getDashboardChart } from "../utils/getDashboardData";
import { generateColorPalette } from "../utils/generateColorPalette";

import type { ICharts, IChartData } from "../types/schema";
import type { IAppPage } from "../types/app";
import type { IKeyValuePair } from "../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const surgeryTypesChart = ref<ICharts>();
const surgeryTypesChartData = ref<IChartData[]>();
const selectedSurgeryType = ref<string>();
const diagnoses = ref<string[]>();
const complicationsChart = ref<ICharts>();
const complicationsChartData = ref<IKeyValuePair>();
const complicationsChartPalette = ref<IKeyValuePair>();
const hasComplicationsData = ref<boolean>(false);
const selectedDiagnosis = ref<string>();
const interventionsChart = ref<ICharts>();
const interventionsChartData = ref<IKeyValuePair>();
const hasInterventionsData = ref<boolean>(false);
const surgeryAgeChart = ref<ICharts>();
const surgeryAgeChartData = ref<IChartData[]>();
const hasSurgeryAgeData = ref<boolean>(false);

async function getPageData() {
  const surgeryTypesResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-type-of-surgery"
  );

  const complicationsResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-surgical-complications"
  );

  const interventionsResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-surgical-interventions"
  );

  const surgeryAgeResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-age-at-first-surgery"
  );

  surgeryTypesChart.value = surgeryTypesResponse[0];
  complicationsChart.value = complicationsResponse[0];
  interventionsChart.value = interventionsResponse[0];
  surgeryAgeChart.value = surgeryAgeResponse[0];
}

function onSurgeryTypesChartClick(value: string) {
  selectedSurgeryType.value = JSON.parse(value).dataPointName!;
  updateComplicationsChart();
}

function updateComplicationsChart() {
  const filteredData = complicationsChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedSurgeryType.value;
    }
  );

  complicationsChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointName",
    "dataPointValue"
  );

  complicationsChartPalette.value = generateColorPalette(
    Object.keys(complicationsChartData.value)
  );

  const sum: number = sumObjectValues(complicationsChartData.value);
  hasComplicationsData.value = sum > 0;
}

function updateInterventionsChart() {
  const filteredData = interventionsChart.value?.dataPoints!.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedDiagnosis.value;
    }
  );

  interventionsChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointValueLabel",
    "dataPointValue"
  );

  const sum: number = sumObjectValues(interventionsChartData.value);
  hasInterventionsData.value = sum > 0;
}

function updateSurgeryAgeChart() {
  surgeryAgeChartData.value = surgeryAgeChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedDiagnosis.value;
    })
    .sort((a: IChartData, b: IChartData) => {
      return (a.dataPointTime as number) - (b.dataPointTime as number);
    });

  const chartAxis = generateAxisTickData(
    surgeryAgeChartData.value,
    "dataPointValue"
  );

  (surgeryAgeChart.value as ICharts).yAxisMaxValue = chartAxis.limit;
  (surgeryAgeChart.value as ICharts).yAxisTicks = chartAxis.ticks;
  hasSurgeryAgeData.value =
    sum(surgeryAgeChartData.value, "dataPointValue") > 0;
}

onMounted(() => {
  getPageData()
    .then(() => {
      // set surgery types data
      surgeryTypesChartData.value = surgeryTypesChart.value?.dataPoints?.sort(
        (a, b) => {
          return a.dataPointOrder! - b.dataPointOrder!;
        }
      );

      const surgeryTypesAxis = generateAxisTickData(
        surgeryTypesChartData.value,
        "dataPointValue"
      );

      (surgeryTypesChart.value as ICharts).yAxisMaxValue =
        surgeryTypesAxis.limit;
      (surgeryTypesChart.value as ICharts).yAxisTicks = surgeryTypesAxis.ticks;

      selectedSurgeryType.value = surgeryTypesChartData.value![0].dataPointName;

      // set diagnoses and starting value
      diagnoses.value = uniqueValues(
        interventionsChart.value?.dataPoints,
        "dataPointPrimaryCategory"
      );
      selectedDiagnosis.value = "ORPHA:87";
    })
    .then(() => {
      updateComplicationsChart();
      updateInterventionsChart();
      updateSurgeryAgeChart();
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => (loading.value = false));
});
</script>
