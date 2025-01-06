<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for your center</h2>
    <h3 class="dashboard-h3">Surgical complications</h3>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel id="surgeryTypeInput" label="Select a type of surgery" />
        <select
          id="surgeryTypeInput"
          v-model="selectedSurgeryType"
          @change="updateComplicationsChart"
        >
          <option v-for="surgeryType in surgeryTypes" :value="surgeryType">
            {{ surgeryType }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 200px" />
        <GroupedColumnChart
          v-else
          :chartId="complicationsChart?.chartId"
          :title="complicationsChart?.chartTitle"
          :description="complicationsChart?.chartSubtitle"
          :chartData="complicationsChartData"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :columnColorPalette="ernCenterPalette"
          :xAxisLabel="complicationsChart?.xAxisLabel"
          :yAxisLabel="complicationsChart?.yAxisLabel"
          :yMin="0"
          :yMax="complicationsChart?.yAxisMaxValue"
          :yTickValues="complicationsChart?.yAxisTicks"
          :chartHeight="200"
          :chartMargins="{
            top: complicationsChart?.topMargin,
            right: complicationsChart?.rightMargin,
            bottom: complicationsChart?.bottomMargin,
            left: complicationsChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <h2 class="dashboard-h2">Surgical interventions by diagnosis</h2>
    <DashboardRow :columns="1">
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
    <DashboardRow :columns="1">
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
          :chartHeight="215"
          :chartScale="0.85"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 225px" />
        <GroupedColumnChart
          v-else
          :chartId="surgeryAgeChart?.chartId"
          :title="surgeryAgeChart?.chartTitle"
          :description="surgeryAgeChart?.chartSubtitle"
          :chartData="surgeryAgeChartData"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointTime"
          :columnColorPalette="ernCenterPalette"
          :xAxisLabel="surgeryAgeChart?.xAxisLabel"
          :yAxisLabel="surgeryAgeChart?.yAxisLabel"
          :yMin="0"
          :yMax="surgeryAgeChart?.yAxisMaxValue"
          :yTickValues="surgeryAgeChart?.yAxisTicks"
          :columnPaddingInner="0.2"
          :columnPaddingOuter="0.3"
          :chartHeight="225"
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
  GroupedColumnChart,
  InputLabel,
  PieChart2,
  LoadingScreen,
  MessageBox,
  // @ts-expect-error
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../utils/generateAxisTicks";
import { asKeyValuePairs, uniqueValues, ernCenterPalette } from "../utils";
import { getDashboardChart } from "../utils/getDashboardData";

import type { ICharts, IChartData } from "../types/schema";
import type { IAppPage } from "../types/app";
import type { IKeyValuePair } from "../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const surgeryTypes = ref<string[]>();
const selectedSurgeryType = ref<string>();
const diagnoses = ref<string[]>();
const selectedDiagnosis = ref<string>();
const complicationsChart = ref<ICharts>();
const complicationsChartData = ref<IChartData[]>();
const interventionsChart = ref<ICharts>();
const interventionsChartData = ref<IKeyValuePair>();
const hasInterventionsData = ref<boolean>(false);
const surgeryAgeChart = ref<ICharts>();
const surgeryAgeChartData = ref<IChartData[]>();

async function getPageData() {
  const centerComplicationsResponse = await getDashboardChart(
    props.api.graphql.current,
    "cs-provider-surgical-complications"
  );
  const ernComplicationsResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-surgical-complications"
  );

  const centerInterventionsResponse = await getDashboardChart(
    props.api.graphql.current,
    "cs-provider-surgical-interventions"
  );

  const ernInterventionsResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-surgical-interventions"
  );

  const centerAgeResponse = await getDashboardChart(
    props.api.graphql.current,
    "cs-provider-age-at-first-surgery"
  );

  const ernAgeResponse = await getDashboardChart(
    props.api.graphql.providers,
    "cs-all-centers-age-at-first-surgery"
  );

  complicationsChart.value = centerComplicationsResponse[0];
  complicationsChart.value.dataPoints = [
    ...(centerComplicationsResponse[0].dataPoints as IChartData[]),
    ...(ernComplicationsResponse[0].dataPoints as IChartData[]),
  ] as IChartData[];

  interventionsChart.value = centerInterventionsResponse[0];
  interventionsChart.value.dataPoints = [
    ...(centerInterventionsResponse[0].dataPoints as IChartData[]),
    ...(ernInterventionsResponse[0].dataPoints as IChartData[]),
  ];

  surgeryAgeChart.value = centerAgeResponse[0];
  surgeryAgeChart.value.dataPoints = [
    ...(centerAgeResponse[0].dataPoints as IChartData[]),
    ...(ernAgeResponse[0].dataPoints as IChartData[]),
  ] as IChartData[];
}

function updateComplicationsChart() {
  complicationsChartData.value = complicationsChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedSurgeryType.value;
    }
  );

  const chartAxis = generateAxisTickData(
    complicationsChartData.value,
    "dataPointValue"
  );
  (complicationsChart.value as ICharts).yAxisMaxValue = chartAxis.limit;
  (complicationsChart.value as ICharts).yAxisTicks = chartAxis.ticks;
}

function updateInterventionsChart() {
  const filteredData = interventionsChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedDiagnosis.value;
    }
  );

  interventionsChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointValueLabel",
    "dataPointValue"
  );

  const values: number[] = Object.keys(interventionsChartData.value).map(
    (key: string) => parseInt(interventionsChartData.value![key])
  );
  const sum: number = values.reduce((acc, value) => acc + value, 0);
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
}

onMounted(() => {
  getPageData()
    .then(() => {
      // sort datasets
      (complicationsChart.value as ICharts).dataPoints = (
        complicationsChart.value as ICharts
      ).dataPoints?.sort((a: IChartData, b: IChartData) => {
        return a.dataPointName?.localeCompare(
          b.dataPointName as string
        ) as number;
      });

      (interventionsChart.value as ICharts).dataPoints = (
        interventionsChart.value as ICharts
      ).dataPoints?.sort((a: IChartData, b: IChartData) => {
        return a.dataPointName?.localeCompare(
          b.dataPointName as string
        ) as number;
      });

      // get surgery types and set starting value
      surgeryTypes.value = uniqueValues(
        (complicationsChart.value as ICharts).dataPoints,
        "dataPointPrimaryCategory"
      );
      selectedSurgeryType.value = surgeryTypes.value[0];

      // set diagnoses and set starting value
      diagnoses.value = uniqueValues(
        (interventionsChart.value as ICharts).dataPoints,
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
    .finally(() => {
      loading.value = false;
    });
});
</script>
