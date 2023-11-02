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
        <ColumnChart
          chartId="cs-center-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="craniosynostosisTypes"
          xvar="type"
          yvar="count"
          :columnColorPalette="colors.craniosynostosis"
          :chartHeight="300"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h3 class="dashboard-h3">Suture Overview</h3>
    <p class="dashboard-text">
      Click a category in the "Affected suture" chart to view more information.
    </p>
    <DashboardChartLayout :columns="2">
      <DashboardBox>
        <ColumnChart
          chartId="craniosynostosis-affected-sutures"
          title="Affected suture"
          class="chart-axis-x-angled-text"
          :chartData="affectedSuture"
          xvar="category"
          yvar="count"
          :yMax="50"
          :yTickValues="[0,10,20,30,40,50]"
          :columnColorPalette="colors.affectedSuture"
          :chartHeight="275"
          :enableClicks="true"
          @column-clicked="updateSutureTypes"
          :chartMargins="{top: 20, right: 10, bottom: 85, left: 60}"
        />
      </DashboardBox>
      <DashboardBox v-if="showSutureTypes">
        <ColumnChart
          chartId="suture-types"
          class="chart-axis-x-angled-text"
          title="Multiple suture synostosis"
          :chartData="sutureTypes"
          xvar="type"
          yvar="count"
          :columnColorPalette="colors.sutureType"
          :chartHeight="275"
          :chartMargins="{top: 20, right: 10, bottom: 85, left: 60}"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import { DashboardBox, PieChart2, ColumnChart, InputLabel } from "molgenis-viz";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// define random data
import { randomInt } from "d3";
import generateColors from "../utils/palette.js";

let csTotalCases = ref(0);
let craniosynostosisTypes = ref([]);
let affectedSuture = ref([]);
let sutureTypes = ref([]);
let showSutureTypes = ref(false);

let colors = ref({
  craniosynostosis: {},
  affectedSuture: {},
  sutureType: {}
});


/// generate random data for craniosynostosis types
function setCraniosynostosisTypes() {
  const types = [
    "Familial",
    "Iatrogenic",
    "Metabolic",
    "Non-syndromic",
    "Syndromic",
  ];
  colors.value.craniosynostosis = generateColors(types);
  craniosynostosisTypes.value = types.map((type) => {
    return { type: type, count: randomInt(10, 42)() };
  });
}

// generate random data for affected suture
function setAffectedSuture() {
  const types = [
    "Frontosphenoidal",
    "Metopic",
    "Multiple",
    "Sagittal",
    "Unicoronal",
    "Unilambdoid",
  ]
  colors.value.affectedSuture = generateColors(types);
  affectedSuture.value = types.map((category) => {
    return { category: category, count: randomInt(10, 50)() };
  });
}

function setSutureTypes() {
  const types = [
    "bicoronal",
    "bilambdoid",
    "bilambdoid+sagittal",
    "pansynostosis",
    "other",
  ]
  colors.value.sutureType = generateColors(types);
  sutureTypes.value = types.map((type) => {
    return { type: type, count: randomInt(3, 27)() };
  });
}

// set update sutute type selection
function updateSutureTypes(value) {
  const total = JSON.parse(value).count;
  const types = sutureTypes.value.map(entry => entry.type);
  let currentTotal = total;
  sutureTypes.value = types
    .map((type, i) => {
      const randomValue =  i === types.length - 1
        ? currentTotal
        : randomInt(1, currentTotal)();
      currentTotal -= randomValue;
      return {type: type, count: randomValue}; 
    });
  console.log(sutureTypes.value)
  showSutureTypes.value = true;
}

function onYearOfBirthFilter() {
  setCraniosynostosisTypes();
  setAffectedSuture();
  setSutureTypes();
}

setCraniosynostosisTypes();
setAffectedSuture();
setSutureTypes();


</script>
