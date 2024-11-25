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

import type { ICharts, IChartData } from "../interfaces/schema";
import type { IAppPage } from "../interfaces/app";
import type { IKeyValuePair } from "../interfaces";
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

  patientsByWorkstreamChart.value.dataPoints =
    patientsByWorkstreamChart.value.dataPoints?.sort((current, next) => {
      return next.dataPointValue! - current.dataPointValue!;
    });

  patientsByWorkstreamChartData.value = asKeyValuePairs(
    data[0].dataPoints,
    "dataPointName",
    "dataPointValue"
  );

  patientsByWorkstreamPalette.value = generateColorPalette(
    Object.keys(patientsByWorkstreamChartData.value).sort()
  );
}

async function getSexByWorksteam() {
  const data = await getDashboardChart(
    props.api.graphql,
    "patients-by-sex-at-birth-and-workstream"
  );
  sexByWorkstreamChart.value = data[0];

  const categories = uniqueValues(
    sexByWorkstreamChart.value.dataPoints,
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

// import { randomInt } from "d3";
// import generateColors from "../utils/palette.js";
// generate random data for display purposes
// let totPatientsSubmitted = ref(0);
// let avgPatientsSubmitted = ref(0);
// let numPatientsByWorkstream = ref({
//   "Cleft lip and palate": 0,
//   Craniosynostosis: 0,
//   "Genetic Deafness": 0,
//   Larynxcleft: 0,
// });
// let showSexAtBirth = ref(false);
// let sexAtBirth = ref({
//   Female: 0,
//   Male: 0,
//   Undetermined: 0,
// });

// let sexAtBirthTitle = ref(null);
// let patientsSexByWorkstream = ref([]);

// const workstreamColors = generateColors(
//   Object.keys(numPatientsByWorkstream.value)
// );
// const genderColors = generateColors(Object.keys(sexAtBirth.value));

// function generatePatients() {
//   const workstreams = Object.keys(numPatientsByWorkstream.value);
//   const sexAtBirthGroups = Object.keys(sexAtBirth.value);
//   const patientsByWorkstream = workstreams.map((value) => [
//     value,
//     randomInt(25, 225)(),
//   ]);

//   numPatientsByWorkstream.value = Object.fromEntries(
//     patientsByWorkstream.sort((a, b) => (a[1] < b[1] ? 1 : -1))
//   );
//   totPatientsSubmitted.value = patientsByWorkstream
//     .map((arr) => arr[1])
//     .reduce((sum, value) => sum + value, 0);

//   avgPatientsSubmitted.value = Math.round(totPatientsSubmitted.value / 12);

//   patientsSexByWorkstream.value = workstreams.map((workstream) => {
//     let currentTotal = numPatientsByWorkstream.value[workstream];
//     const sexByWorkstream = sexAtBirthGroups.map((group, i) => {
//       const randomValue =
//         i === sexAtBirthGroups.length - 1
//           ? currentTotal
//           : randomInt(1, currentTotal)();
//       const row = [group, randomValue];
//       currentTotal -= randomValue;
//       return row;
//     });
//     return {
//       workstream: workstream,
//       data: Object.fromEntries(
//         sexByWorkstream.sort((a, b) => (a[1] < b[1] ? 1 : -1))
//       ),
//     };
//   });
// }

// generatePatients();

// function updateSelection(value) {
//   const workstream = Object.keys(value)[0];
//   sexAtBirthTitle.value = `Sex at birth for ${workstream} patients`;
//   sexAtBirth.value = patientsSexByWorkstream.value.filter(
//     (row) => row.workstream === workstream
//   )[0].data;
//   showSexAtBirth.value = true;
// }
</script>
