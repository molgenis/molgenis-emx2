<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for all centers</h2>
    <h3 class="dashboard-h3">Overview of all surgical interventions</h3>
    <DashboardChartLayout :columns="2" class="dashboard-boxes-width-2-1">
      <DashboardBox>
        <ColumnChart
          chartId="cs-all-surgical-type-of-surgery"
          title="Type of surgery"
          description="Click a type of surgery to view complications"
          :chartData="typeOfSurgery"
          xvar="type"
          yvar="count"
          :yTickValues="[0,25,50,75,100]"
          :yMax="100"
          :columnColorPalette="surgeryTypeColors"
          xAxisLineBreaker=" "
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-complications"
          :title="surgicalComplicationsTitle"
          :chartData="surgicalComplications"
          :chartColors="complicationColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :stackLegend="true"
          :chartHeight="200"
          :chartScale="0.9"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h2 class="dashboard-h2">Surgical interventions by diagnosis</h2>
    <DashboardBox class="mb-4">
      <InputLabel id="diagnosisInput" label="Select a diagnosis" />
      <select id="diagnosisInput" @change="onDiagnosisInput">
        <option value="ORPHA:87">Apert syndrome</option>
        <option value="ORPHA:207">Crouzon syndrome</option>
        <option value="ORPHA:93262">
          Crouzon syndrome-acanthosis nigricans syndrome
        </option>
        <option value="OSNEW1">ERF-related craniosynostosis syndrome</option>
        <option value="ORPHA:53271">Muenke syndrome</option>
        <option value="ORPHA:3366">
          Non-syndromic metopic craniosynostosis
        </option>
        <option value="ORPHA:35093">
          Non-syndromic sagittal craniosynostosis
        </option>
        <option value="ORPHA:620102">
          Non-syndromic unicoronal craniosynostosis
        </option>
        <option value="ORPHA:620113">
          Non-syndromic unilambdoid craniosynostosis
        </option>
        <option value="ORPHA:794">Saethre-Chotzen syndrome</option>
        <option value="OSNEW5">TCF12-related craniosynostosis</option>
        <option value="ORPHA:620198">
          non-syndromic multistural craniosynostosis
        </option>
      </select>
    </DashboardBox>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-interventions"
          title="Surgical Interventions"
          :chartData="surgicalInterventions"
          :chartColors="surgicalInterventionColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          :stackLegend="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
          :valuesArePercents="false"
        />
      </DashboardBox>
      <DashboardBox>
        <ColumnChart
          chartId="cd-all-surgical-age-at-surgery"
          title="Age at first surgery"
          description="Number of patients by age (months)"
          :chartData="ageAtFirstSurgery"
          xvar="age"
          yvar="value"
          xAxisLabel="Age (months)"
          yAxisLabel="Number of patients"
          :yMax="200"
          :yTickValues="[0, 25, 50, 75, 100, 125, 150, 175, 200]"
          :chartHeight="280"
          columnFill="#2a8f64"
          columnHoverFill="#ed7b23"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, PieChart2, ColumnChart, InputLabel } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

// create random datasets for demo purposes
import { randomInt } from "d3";
import generateColors from "../utils/palette.js";
import { seq } from "../utils/devtools.js";

let typeOfSurgery = ref({
  "Extracranial procedures": 0,
  Hydrocephalus: 0,
  Midface: 0,
  Vault: 0,
});
let totalCases = ref(0);
let surgicalComplications = ref({
  Complications: 0,
  "No complications": 0,
});
let surgicalComplicationsTitle = ref("All surgical complications");
let surgicalInterventions = ref({
  "Additional planned surgery according to protocol": 0,
  "First surgery": 0,
  "Unwanted reoperation due to complications": 0,
});

let ageAtFirstSurgery = ref([]);

let surgeryTypeColors = ref({});
const complicationColors = generateColors(
  Object.keys(surgicalComplications.value)
);
const surgicalInterventionColors = generateColors(
  Object.keys(surgicalInterventions.value)
);

function setTypeOfSurgery() {
  const types = ["Extracranial procedures", "Hydrocephalus", "Midface", "Vault"];
  surgeryTypeColors.value = generateColors(types)
  typeOfSurgery.value = types.map((type) => {
    return { type: type, count: randomInt(1, 100)() }
  })

  totalCases.value = typeOfSurgery.value
    .map((row) => row.count)
    .reduce((sum, value) => sum + value, 0); 
}

function setSurgicalComplications(total) {
  const types = Object.keys(surgicalComplications.value);
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
  surgicalComplications.value = Object.fromEntries(data);
}

function updateSurgicalComplications(value) {
  const title = Object.keys(value);
  surgicalComplicationsTitle.value = `${title} surgical complications`;
  const total = value[Object.keys(value)];
  setSurgicalComplications(total);
}

function setSurgicalInterventions() {
  const types = Object.keys(surgicalInterventions.value);
  let currentTotal = totalCases.value;
  const data = types.map((type, i) => {
    const randomValue =
      i === types.length - 1 ? currentTotal : randomInt(1, currentTotal)();
    const row = [type, randomValue];
    currentTotal -= randomValue;
    return row;
  });
  surgicalInterventions.value = Object.fromEntries(data);
}

function setAgeAtFirstSurgery() {
  const ages = seq(0, 14, 2);
  let currentTotal = totalCases.value;
  const data = ages.map((age, i) => {
    const randomValue =
      i === ages.length - 1 ? currentTotal : randomInt(1, currentTotal)();
    const row = { age: `${age}`, value: randomValue };
    currentTotal -= randomValue;
    return row;
  });
  ageAtFirstSurgery.value = data;
}

function onDiagnosisInput() {
  setSurgicalInterventions(totalCases.value);
  setAgeAtFirstSurgery();
}

setTypeOfSurgery();
setSurgicalComplications(totalCases.value);
setSurgicalInterventions();
setAgeAtFirstSurgery();
</script>
