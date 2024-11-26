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
        <LoadingScreen v-if="loading" />
        <ValueShowcase
          v-else
          :title="`${numberOfPatientsSubmitted} patients submitted`"
          :description="`Your center has submitted on average ${monthlyAverageOfSubmissions} patients per month`"
        >
          <template v-slot:icon>
            <UserCircleIcon />
          </template>
        </ValueShowcase>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart id="provider-overview-patients-by-workstream">
        <LoadingScreen v-if="loading" />
        <PieChart2
          v-else
          :chartId="patientsByWorkstreamChart?.chartId"
          :title="patientsByWorkstreamChart?.chartTitle"
          :description="patientsByWorkstreamChart?.chartSubtitle"
          :chartData="patientsByWorkstreamChartData"
          :chartColors="patientsByWorkstreamPalette"
          :valuesArePercents="false"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :enableClicks="true"
          :chartHeight="215"
          :chartScale="0.85"
          @slice-clicked="updateSexByWorkstream"
        />
      </DashboardChart>
      <DashboardChart v-if="selectedWorkstream">
        <PieChart2
          :chartId="sexByWorkstreamChart?.chartId"
          :title="`${sexByWorkstreamChart?.chartTitle} for ${selectedWorkstream} patients`"
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
import { ref, onBeforeMount } from "vue";
import { UserCircleIcon } from "@heroicons/vue/24/outline";
import {
  DashboardRow,
  DashboardChart,
  PieChart2,
  LoadingScreen,
  // @ts-ignore
} from "molgenis-viz";

import ProviderDashboard from "../components/ProviderDashboard.vue";
import ValueShowcase from "../components/ValueShowcase.vue";

import { asKeyValuePairs, uniqueValues } from "../utils";
import { generateColorPalette } from "../utils/generateColorPalette";
import { getDashboardChart } from "../utils/getDashboardData";

import type { ICharts, IChartData } from "../types/schema";
import type { IAppPage } from "../types/app";
import type { IKeyValuePair } from "../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const numberOfPatientsSubmitted = ref<number>(0);
const monthlyAverageOfSubmissions = ref<number>(0);
const patientsByWorkstreamChart = ref<ICharts>();
const patientsByWorkstreamChartData = ref<IKeyValuePair>();
const patientsByWorkstreamPalette = ref<IKeyValuePair>();
const sexByWorkstreamChart = ref<ICharts>();
const sexByWorkstreamChartData = ref<IKeyValuePair>();
const sexByWorkstreamPalette = ref<IKeyValuePair>();
const selectedWorkstream = ref<string>();

async function getSubmissionData() {
  const patientSubmissions = await getDashboardChart(
    props.api.graphql,
    "patients-total"
  );
  const data = patientSubmissions[0];
  if (data.dataPoints) {
    numberOfPatientsSubmitted.value = data.dataPoints[0].dataPointValue!;
  }
}

async function getAverageData() {
  const avgData = await getDashboardChart(
    props.api.graphql,
    "patients-per-month"
  );

  const data = avgData[0];
  if (data.dataPoints) {
    monthlyAverageOfSubmissions.value = data.dataPoints[0].dataPointValue!;
  }
}

async function getPatientsByWorkstream() {
  const data = await getDashboardChart(
    props.api.graphql,
    "patients-by-workstream"
  );
  patientsByWorkstreamChart.value = data[0];
  const chartDataPoints = data[0].dataPoints as IChartData[];

  patientsByWorkstreamChart.value.dataPoints = chartDataPoints.sort(
    (current, next) => {
      return next.dataPointValue! - current.dataPointValue!;
    }
  );

  const workstreamData = asKeyValuePairs(
    chartDataPoints,
    "dataPointName",
    "dataPointValue"
  );

  patientsByWorkstreamPalette.value = generateColorPalette(
    Object.keys(workstreamData).sort()
  );

  patientsByWorkstreamChartData.value = workstreamData;
}

async function getSexByWorksteam() {
  const data = await getDashboardChart(
    props.api.graphql,
    "patients-by-sex-at-birth-and-workstream"
  );
  sexByWorkstreamChart.value = data[0];

  const categories = uniqueValues(
    sexByWorkstreamChart.value?.dataPoints,
    "dataPointName"
  );
  sexByWorkstreamPalette.value = generateColorPalette(categories);
}

function updateSexByWorkstream(value: string) {
  selectedWorkstream.value = Object.keys(value)[0];
  const filteredData = sexByWorkstreamChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedWorkstream.value;
    })
    .sort((current, next) => {
      return next.dataPointValue! - current.dataPointValue!;
    });

  sexByWorkstreamChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointName",
    "dataPointValue"
  );
}

onBeforeMount(async () => {
  try {
    await getSubmissionData();
    await getAverageData();
    await getPatientsByWorkstream();
    await getSexByWorksteam();
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
});
</script>
