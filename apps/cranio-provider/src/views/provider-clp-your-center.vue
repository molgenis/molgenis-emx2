<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">
      Overview of patients {{ ageGroupFilter }} years old (n={{ totalCases }})
    </h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <h3>Options</h3>
        <InputLabel
          id="yearOfBirthFilter"
          label="Year of birth"
          description="Limit the results by year of birth"
        />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          @change="onYearOfBirthFilter"
        >
          <option value="3-4" selected>3-4 years</option>
          <option value="5-6">5-6 years</option>
          <option value="8-9">8-9 years</option>
          <option value="10-12">10-12 years</option>
          <option value="18+">18+ years</option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart id="clp-patients-by-phenotype">
        <ColumnChart
          chartId="patientsByPhenotype"
          title="Patients by phenotype"
          :chartData="patientsByPhenotype"
          xvar="type"
          yvar="count"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :chartHeight="250"
          :column-color-palette="phenotypeColors"
          columnHoverFill="#708fb4"
        />
      </DashboardChart>
      <DashboardChart>
        <PieChart2
          chartId="patientsByGender"
          title="Patients per gender"
          :chartData="patientsByGender"
          :asDonutChart="true"
          :chartColors="genderColors"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
          :valuesArePercents="false"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart id="clp-cleft-q-completed" v-if="showCleftQCompleted">
        <ProgressMeter
          chartId="cleftQCompleted"
          :title="`% of patients that completed the CLEFT-Q (${ageGroupFilter}yrs)`"
          :value="cleftQCompleted"
          :totalValue="totalCases"
          :barHeight="25"
          barFill="#66c2a4"
        />
      </DashboardChart>
      <DashboardChart id="clp-ics-completed" v-if="showIcsCompleted">
        <ProgressMeter
          chartId="icsCompleted"
          :title="`% of patients that completed the ICS (${ageGroupFilter}yrs)`"
          :value="icsCompleted"
          :totalValue="totalCases"
          :barHeight="25"
          barFill="#9f6491"
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
  ColumnChart,
  PieChart2,
  ProgressMeter,
  InputLabel,
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

// generate random data for display purposes
import { randomInt } from "d3";
import generateColors from "../utils/palette.js";

let totalCases = ref(0);
let patientsByPhenotype = ref([]);
let patientsByGender = ref({ Female: 0, Male: 0, Undetermined: 0 });
let cleftQCompleted = ref(0);
let icsCompleted = ref(0);
let showCleftQCompleted = ref(false);
let showIcsCompleted = ref(false);
let ageGroupFilter = ref("3-4");

const phenotypeColors = generateColors(["CL", "CLA", "CP", "CLAP"]);
const genderColors = generateColors(Object.keys(patientsByGender.value));

function setPatientsByPhenotype() {
  const types = ["CL", "CLA", "CP", "CLAP"];
  const data = types
    .map((type) => Object.assign({ type: type, count: randomInt(1, 100)() }))
    .sort((current, next) => (current.type < next.type ? 1 : -1));

  totalCases.value = data
    .map((row) => row.count)
    .reduce((sum, value) => sum + value, 0);
  patientsByPhenotype.value = data;
}

function setPatientsByGender() {
  let currentTotal = totalCases.value;
  const groups = Object.keys(patientsByGender.value);
  const data = groups
    .map((type, i) => {
      const value =
        i === groups.length - 1
          ? currentTotal
          : i === 0
          ? Math.round(currentTotal * 0.35)
          : randomInt(1, currentTotal)();
      const row = [type, value];
      currentTotal -= value;
      return row;
    })
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  patientsByGender.value = Object.fromEntries(data);
}

function setProgressMeters() {
  showCleftQCompleted.value = ["8-9", "10-12", "18+"].includes(
    ageGroupFilter.value
  );
  showIcsCompleted.value = ["3-4", "5-6"].includes(ageGroupFilter.value);
  cleftQCompleted.value = randomInt(1, totalCases.value)();
  icsCompleted.value = randomInt(1, totalCases.value)();
}

function onYearOfBirthFilter(event) {
  ageGroupFilter.value = event.target.value;
  setPatientsByPhenotype();
  setPatientsByGender();
  setProgressMeters();
}

setPatientsByPhenotype();
setPatientsByGender();
setProgressMeters();
</script>
