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
        <LoadingScreen v-if="loading" style="height: 215px" />
        <MessageBox v-else-if="!loading && numberOfPatientsSubmitted === 0">
          <span>Not enough data to show chart</span>
        </MessageBox>
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
import { ref, onMounted } from "vue";
import { UserCircleIcon } from "@heroicons/vue/24/outline";
import {
  DashboardRow,
  DashboardChart,
  PieChart2,
  LoadingScreen,
  MessageBox,
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

async function getPageData() {
  const submissionsResponse = await getDashboardChart(
    props.api.graphql.current,
    "patients-total"
  );

  const monthlySubmissionResponse = await getDashboardChart(
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

  const submissionsData = submissionsResponse[0];
  const monthlySubmissionsData = monthlySubmissionResponse[0];
  numberOfPatientsSubmitted.value =
    submissionsData.dataPoints![0].dataPointValue!;
  monthlyAverageOfSubmissions.value =
    monthlySubmissionsData.dataPoints![0].dataPointValue!;
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

onMounted(() => {
  getPageData()
    .then(() => {
      // prepare workstream charts
      patientsByWorkstreamChart.value!.dataPoints =
        patientsByWorkstreamChart.value?.dataPoints?.sort(
          (a: IChartData, b: IChartData) => {
            return (b.dataPointValue as number) - (a.dataPointValue as number);
          }
        );
      patientsByWorkstreamChartData.value = asKeyValuePairs(
        patientsByWorkstreamChart.value?.dataPoints,
        "dataPointName",
        "dataPointValue"
      );
      patientsByWorkstreamPalette.value = generateColorPalette(
        Object.keys(patientsByWorkstreamChartData.value).sort()
      );

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
