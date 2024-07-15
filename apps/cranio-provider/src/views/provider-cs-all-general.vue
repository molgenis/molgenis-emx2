<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for all centers</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel
          id="yearOfBirthFilter"
          label="Filter data by year of birth"
        />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          @change="onYearOfBirthFilter"
        >
          <option value="all">All Patients</option>
          <option value="2023" selected>2023</option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart id="type-of-craniosynostosis">
        <ColumnChart
          chartId="craniosynostosis-types"
          title="Type of craniosynostosis"
          :chartData="craniosynostosisTypes"
          xvar="type"
          yvar="count"
          :columnColorPalette="colors.craniosynostosis"
          :chartHeight="225"
        />
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Suture Overview</h3>
    <p class="dashboard-text">
      Click a category in the "Affected suture" chart to view more information.
    </p>
    <DashboardRow :columns="2">
      <DashboardChart>
        <ColumnChart
          chartId="craniosynostosis-affected-sutures"
          title="Affected suture"
          class="chart-axis-x-angled-text"
          :chartData="affectedSuture"
          xvar="category"
          yvar="count"
          :yMax="50"
          :yTickValues="[0, 10, 20, 30, 40, 50]"
          :columnColorPalette="colors.affectedSuture"
          :chartHeight="275"
          :enableClicks="true"
          @column-clicked="updateSutureTypes"
          :chartMargins="{ top: 20, right: 10, bottom: 85, left: 60 }"
        />
      </DashboardChart>
      <DashboardChart v-if="showSutureTypes">
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
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Patients Overview</h3>
    <DashboardRow :columns="1">
      <DashboardChart>
        <ColumnChart
          chartId="countryOfResidence"
          title="Patients by country of residence"
          description="Total number of patients residing in each country"
          :chartData="countryOfResidence"
          xvar="country"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :chartHeight="225"
          :columnColorPalette="colors.countries"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import {
  DashboardRow,
  DashboardChart,
  InputLabel,
  ColumnChart,
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// generate random data
import { randomInt } from "d3";
import generateColors from "../utils/palette";

let craniosynostosisTypes = ref([]);
let countryOfResidence = ref([]);
let affectedSuture = ref([]);
let sutureTypes = ref([]);
let showSutureTypes = ref(false);

let colors = ref({
  craniosynostosis: {},
  affectedSuture: {},
  sutureType: {},
  countries: {},
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
  ];
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
  ];
  colors.value.sutureType = generateColors(types);
  sutureTypes.value = types.map((type) => {
    return { type: type, count: randomInt(3, 27)() };
  });
}

// generate random data for "country of residence"
function setCountryOfResidence() {
  const types = ["NL", "FR", "GE", "ES", "PO", "SE"];
  colors.value.countries = generateColors(types);
  countryOfResidence.value = types
    .map((country) => {
      return { country: country, value: randomInt(10, 100)() };
    })
    .sort((a, b) => (a.country < b.country ? -1 : 1));
}

// set update sutute type selection
function updateSutureTypes(value) {
  const total = JSON.parse(value).count;
  const types = sutureTypes.value.map((entry) => entry.type);
  let currentTotal = total;
  sutureTypes.value = types.map((type, i) => {
    const randomValue =
      i === types.length - 1 ? currentTotal : randomInt(1, currentTotal)();
    currentTotal -= randomValue;
    return { type: type, count: randomValue };
  });
  console.log(sutureTypes.value);
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

<style lang="scss">
.chart-axis-x-angled-text {
  .chart-axes {
    .chart-axis-x {
      .tick {
        text {
          @media (min-width: 835px) {
            transform: rotate(-20deg);
            transform-origin: 0 -10%;
            text-anchor: end;
          }
        }
      }
    }
  }
}
</style>
