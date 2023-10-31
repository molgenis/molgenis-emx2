<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for all centers</h2>
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
      <DashboardBox id="type-of-craniosynostosis">
        <PieChart2
          chartId="cs-types"
          title="Type of craniosynostosis"
          :chartData="craniosynostosisTypes"
          :chartColors="craniosynostosisColors"
          :chartHeight="300"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="right"
          :stackLegend="true"
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
          chartId="cs-affected-sutures"
          title="Affected suture"
          :chartData="affectedSuture"
          :chartHeight="200"
          :asDonutChart="true"
          :chartColors="singleSutureColors"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.9"
          :valuesArePercents="false"
          :enableClicks="true"
          @slice-clicked="updateSutureTypes"
        />
      </DashboardBox>
      <DashboardBox v-if="showSutureTypes">
        <PieChart2
          chartId="sutureTypes"
          title="Multiple suture synostosis"
          :chartData="sutureTypes"
          :chartHeight="200"
          :chartColors="multipleSutureColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <DashboardChartLayout :columns="1">
      <DashboardBox>
        <ColumnChart
          chartId="countryOfResidence"
          title="Patients by country of residence"
          :chartData="countryOfResidence"
          xvar="country"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :chartHeight="225"
          columnFill="#2a8f64"
          columnHoverFill="#ed7b23"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, PieChart2, InputLabel, ColumnChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// generate random data
import { randomInt } from "d3";
import generateColors from "../utils/palette";

let craniosynostosisTypes = ref({
  Familial: 0,
  Iatrogenic: 0,
  Metabolic: 0,
  "Non-syndromic": 0,
  Syndromic: 0,
});

let countryOfResidence = ref([]);

let affectedSuture = ref({
  Frontosphenoidal: 0,
  Metopic: 0,
  Multiple: 0,
  Sagittal: 0,
  Unicoronal: 0,
  Unilambdoid: 0,
});

let sutureTypes = ref({
  bicoronal: 0,
  bilambdoid: 0,
  "bilambdoid+sagittal": 0,
  pansynostosis: 0,
  other: 0,
});

let showSutureTypes = ref(false);

const craniosynostosisColors = generateColors(
  Object.keys(craniosynostosisTypes.value)
);
const singleSutureColors = generateColors(Object.keys(affectedSuture.value));
const multipleSutureColors = generateColors(Object.keys(sutureTypes.value));

/// generate random data for craniosynostosis types
function setCraniosynostosisTypes() {
  const types = Object.keys(craniosynostosisTypes.value);
  const data = types
    .map((type) => [type, randomInt(10, 42)()])
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  craniosynostosisTypes.value = Object.fromEntries(data);
}

// generate random data for affected suture
function setAffectedSuture() {
  const categories = Object.keys(affectedSuture.value);
  const data = categories
    .map((category) => [category, randomInt(10, 60)()])
    .sort((a, b) => (a[1] < b[1] ? 1 : -1));
  affectedSuture.value = Object.fromEntries(data);
}

function setSutureTypes() {
  const types = Object.keys(sutureTypes.value);
  const data = types
    .map((type) => [type, randomInt(3, 27)()])
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  sutureTypes.value = Object.fromEntries(data);
}

// generate random data for "country of residence"
function setCountryOfResidence() {
  const countries = ["NL", "FR", "GE", "ES", "PO", "SE"];
  countryOfResidence.value = countries
    .map((country) => {
      return { country: country, value: randomInt(10, 100)() };
    })
    .sort((a, b) => (a.country < b.country ? -1 : 1));
}

// set update sutute type selection
function updateSutureTypes(value) {
  const total = value[Object.keys(value)];
  const types = Object.keys(sutureTypes.value);
  let currentTotal = total;
  const newSutureTypes = types
    .map((type, i) => {
      const randomValue =
        i === types.length - 1 ? currentTotal : randomInt(1, currentTotal)();
      const row = [type, randomValue];
      currentTotal -= randomValue;
      return row;
    })
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  sutureTypes.value = Object.fromEntries(newSutureTypes);
  showSutureTypes.value = true;
}

setCraniosynostosisTypes();
setAffectedSuture();
setCountryOfResidence();
setSutureTypes();

function onYearOfBirthFilter() {
  setCraniosynostosisTypes();
  setAffectedSuture();
  setCountryOfResidence();
  setSutureTypes();
}
</script>
