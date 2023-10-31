<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for your centers</h2>
    <DashboardBox class="mb-4">
      <InputLabel id="yearOfBirthFilter" label="Filter data by year of birth" />
      <select
        class="inputs select"
        id="yearOfBirthFilter"
        @change="onYearOfBirthFilter"
      >
        <option value="all">All Patients</option>
        <option value="2023" selected>2023</option>
      </select>
    </DashboardBox>
    <DashboardChartLayout :columns="1">
      <DashboardBox>
        <PieChart2
          chartId="cs-center-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="csTypes"
          :chartColors="csTypeColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="right"
          :stackLegend="true"
          :chartHeight="300"
          :chartScale="0.65"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h3 class="dashboard-h3">Suture Overview</h3>
    <p class="dashboard-text">
      Click a category in the "Affected suture" chart to view more information.
    </p>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart2
          chartId="cs-center-affect-suture"
          title="Affected suture"
          :chartData="csSingleSutures"
          :chartColors="singleSutureColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.85"
          :valuesArePercents="false"
          :enableClicks="true"
          @slice-clicked="updateMultipleSutures"
        />
      </DashboardBox>
      <DashboardBox v-if="showSutureTypes">
        <PieChart2
          chartId="sutureTypes"
          title="Multiple suture synostosis"
          :chartData="csMultipleSutures"
          :chartColors="multipleSutureColors"
          :chartHeight="200"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import { DashboardBox, PieChart2, InputLabel } from "molgenis-viz";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// define random data
import { randomInt } from "d3";
import generateColors from "../utils/palette.js";

let csTotalCases = ref(0);
let csTypes = ref({
  Familial: 0,
  Iatrogenic: 0,
  Metabolic: 0,
  "Non-syndromic": 0,
  Syndromic: 0,
});
let csSingleSutures = ref({
  Frontosphenoidal: 0,
  Metopic: 0,
  Multiple: 0,
  Sagittal: 0,
  Unicoronal: 0,
  Unilambdoid: 0,
});
let csMultipleSutures = ref({
  bicoronal: 0,
  bilambdoid: 0,
  "bilambdoid+sagittal": 0,
  pansynostosis: 0,
  other: 0,
});
let showSutureTypes = ref(false);

const csTypeColors = generateColors(Object.keys(csTypes.value));
const singleSutureColors = generateColors(Object.keys(csSingleSutures.value));
const multipleSutureColors = generateColors(
  Object.keys(csMultipleSutures.value)
);

function setCsTypes() {
  const types = Object.keys(csTypes.value);
  const data = types
    .map((type) => [type, randomInt(1, 100)()])
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  csTotalCases.value = data
    .map((row) => row[1])
    .reduce((sum, value) => sum + value, 0);
  csTypes.value = Object.fromEntries(data);
}

function setSingleSutures() {
  const types = Object.keys(csSingleSutures.value);
  let currentTotal = csTotalCases.value;
  const data = types
    .map((type, i) => {
      const randomValue =
        i === types.length - 1 ? currentTotal : randomInt(3, currentTotal)();
      const row = [type, randomValue];
      currentTotal -= randomValue;
      return row;
    })
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  csSingleSutures.value = Object.fromEntries(data);
}

function setMultipleSutures(total) {
  const types = Object.keys(csMultipleSutures.value);
  let currentTotal = total;
  const data = types
    .map((type, i) => {
      const randomValue =
        i === types.length - 1 ? currentTotal : randomInt(1, currentTotal)();
      const row = [type, randomValue];
      currentTotal -= randomValue;
      return row;
    })
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  csMultipleSutures.value = Object.fromEntries(data);
}

function updateMultipleSutures(value) {
  const total = value[Object.keys(value)];
  setMultipleSutures(total);
  showSutureTypes.value = true;
}

function onYearOfBirthFilter() {
  setCsTypes();
  setSingleSutures();
}

// generate data
setCsTypes();
setSingleSutures();
</script>
