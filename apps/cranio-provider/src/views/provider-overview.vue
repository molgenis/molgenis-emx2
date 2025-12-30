<template>
  <ProviderDashboard>
    <DashboardRow :columns="1">
      <DashboardChart id="provider-overview-welcome">
        <h2 class="dashboard-title">
          Welcome to <span>{{ organisation?.name }}'s</span> dashboard!
        </h2>
        <p>
          Here you can view an overview of patients your centre has submitted to
          the ERN Cranio registry and compare the results of your centre against
          the entire registry. On the current page, you will find a snapshot of
          your centre and the patients submitted as of today. All visualisations
          are updated daily.
        </p>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart
        id="provider-overview-patients-submitted"
        class="center-showcase"
      >
        <LoadingScreen v-if="loading" style="height: auto" />
        <ValueShowcase
          v-else
          :title="(numberOfPatientsSubmitted?.chartTitle as string)"
          :description="(patientsSubmittedByTime?.chartTitle as string)"
        >
          <template v-slot:icon>
            <UserCircleIcon />
          </template>
        </ValueShowcase>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart id="provider-overview-patients-by-workstream">
        <LoadingScreen v-if="loading" style="height: 215px" />
        <MessageBox
          v-else-if="!loading && numberOfPatientsSubmitted!.dataPoints![0].dataPointValue === 0"
        >
          <span>Not enough data to show chart</span>
        </MessageBox>
        <ColumnChart
          v-else-if="patientsByWorkstreamChart"
          :chartId="patientsByWorkstreamChart.chartId"
          :title="patientsByWorkstreamChart.chartTitle"
          :description="patientsByWorkstreamChart.chartSubtitle"
          :chartData="patientsByWorkstreamChartData"
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="patientsByWorkstreamChart.xAxisLabel"
          :yAxisLabel="patientsByWorkstreamChart.yAxisLabel"
          :yMin="0"
          :yMax="patientsByWorkstreamChart.yAxisMaxValue"
          :yTickValues="patientsByWorkstreamChart.yAxisTicks"
          xAxisLineBreaker=" "
          :columnColorPalette="patientsByWorkstreamPalette"
          :chartHeight="275"
          :chartMargins="{
            top: patientsByWorkstreamChart.topMargin,
            right: patientsByWorkstreamChart.rightMargin,
            bottom: patientsByWorkstreamChart.bottomMargin,
            left: patientsByWorkstreamChart.leftMargin,
          }"
          :enableClicks="true"
          @columnClicked="updateSexByWorkstream"
        />
      </DashboardChart>
      <DashboardChart v-if="selectedWorkstream">
        <PieChart2
          :chartId="sexByWorkstreamChart?.chartId"
          :title="`${sexByWorkstreamChart?.chartTitle} for ${selectedWorkstream.dataPointName} patients`"
          :description="sexByWorkstreamChart?.chartSubtitle"
          :chartData="sexByWorkstreamChartData"
          :chartColors="sexByWorkstreamPalette"
          :valuesArePercents="false"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :enableClicks="true"
          :chartHeight="215"
          :chartScale="0.85"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { UserCircleIcon } from "@heroicons/vue/24/outline";
import {
  DashboardRow,
  DashboardChart,
  PieChart2,
  ColumnChart,
  LoadingScreen,
  MessageBox,
  // @ts-ignore
} from "molgenis-viz";

import ProviderDashboard from "../components/ProviderDashboard.vue";
import ValueShowcase from "../components/ValueShowcase.vue";

import { asKeyValuePairs, uniqueValues } from "../utils";
import { generateColorPalette } from "../utils/generateColorPalette";
import { getDashboardChart } from "../utils/getDashboardData";
import { generateAxisTickData } from "../utils/generateAxisTicks";
import { parseChartTitle } from "../utils/parseChartTitle";

import type { ICharts, IChartData } from "../types/schema";
import type { IAppPage } from "../types/app";
import type { IKeyValuePair } from "../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const numberOfPatientsSubmitted = ref<ICharts>();
const patientsSubmittedByTime = ref<ICharts>();
const patientsByWorkstreamChart = ref<ICharts>();
const patientsByWorkstreamChartData = ref<IChartData[]>();
const patientsByWorkstreamPalette = ref<IKeyValuePair>();
const sexByWorkstreamChart = ref<ICharts>();
const sexByWorkstreamChartData = ref<IKeyValuePair>();
const sexByWorkstreamPalette = ref<IKeyValuePair>();
const selectedWorkstream = ref<IChartData>();

async function getPageData() {
  const submissionsResponse = await getDashboardChart(
    props.api.graphql.current,
    "patients-total"
  );

  const timeSubmissionResponse = await getDashboardChart(
    props.api.graphql.current,
    "patients-per-month"
  );

  const patientsByWorkstreamResponse = await getDashboardChart(
    props.api.graphql.current,
    "patients-by-workstream"
  );

  const sexByWorkstreamResponse = await getDashboardChart(
    props.api.graphql.current,
    "patients-by-sex-at-birth-and-workstream"
  );

  patientsByWorkstreamChart.value = patientsByWorkstreamResponse[0];
  sexByWorkstreamChart.value = sexByWorkstreamResponse[0];

  numberOfPatientsSubmitted.value = submissionsResponse[0];
  patientsSubmittedByTime.value = timeSubmissionResponse[0];
}

function updateSexByWorkstream(value: string) {
  const selection = JSON.parse(value);
  selectedWorkstream.value = selection;
  const filteredData = sexByWorkstreamChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selection.dataPointName;
    })
    .sort((current, next) => {
      return (
        (next.dataPointValue as number) - (current.dataPointValue as number)
      );
    });

  sexByWorkstreamChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointName",
    "dataPointValue"
  );
}

onMounted(() => {
  getPageData()
    .then(() => {
      // set value showcase titles
      if (
        numberOfPatientsSubmitted.value?.chartTitle &&
        numberOfPatientsSubmitted.value?.dataPoints
      ) {
        numberOfPatientsSubmitted.value.chartTitle = parseChartTitle(
          numberOfPatientsSubmitted.value.chartTitle,
          numberOfPatientsSubmitted.value.dataPoints[0].dataPointValue!
        );
      }

      if (
        patientsSubmittedByTime.value?.chartTitle &&
        patientsSubmittedByTime.value?.dataPoints
      ) {
        patientsSubmittedByTime.value.chartTitle = parseChartTitle(
          patientsSubmittedByTime.value.chartTitle,
          patientsSubmittedByTime.value.dataPoints[0].dataPointValue!
        );
      }

      // prepare workstream data
      patientsByWorkstreamChartData.value =
        patientsByWorkstreamChart.value!.dataPoints?.sort(
          (a: IChartData, b: IChartData) =>
            (a.dataPointOrder as number) - (b.dataPointOrder as number)
        );

      const workstreams = uniqueValues(
        patientsByWorkstreamChartData.value,
        "dataPointName"
      );
      patientsByWorkstreamPalette.value = generateColorPalette(workstreams);

      const chartTicks = generateAxisTickData(
        patientsByWorkstreamChartData.value!,
        "dataPointValue"
      );
      patientsByWorkstreamChart.value!.yAxisMaxValue = chartTicks.limit;
      patientsByWorkstreamChart.value!.yAxisTicks = chartTicks.ticks;

      // prepare sex at birth chart
      const uniqueSexAtBirthCategories = uniqueValues(
        sexByWorkstreamChart.value?.dataPoints,
        "dataPointName"
      );
      sexByWorkstreamPalette.value = generateColorPalette(
        uniqueSexAtBirthCategories
      );
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => (loading.value = false));
});
</script>
