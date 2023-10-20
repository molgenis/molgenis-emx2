<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for all centers</h2>
    <h3 class="dashboard-h3">Overview of all surgical interventions</h3>
    <DashboardChartLayout :columns="2">
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-type-of-surgery"
          title="Type of surgery"
          :chartData="typeOfSurgery"
          :chartColors="surgeryTypeColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
          :valuesArePercents="false"
          :enableClicks="true"
          @slice-clicked="updateSurgicalComplications"
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
          :chartHeight="200"
          :chartScale="0.9"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout>
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
          :chartData="ageAtSurgery"
          xvar="age"
          yvar="value"
          xAxisLabel="Age (months)"
          yAxisLabel="Number of patients"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
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
import { DashboardBox, PieChart2, ColumnChart } from "molgenis-viz";
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

let ageAtSurgery = ref(
  seq(0, 14, 2).map((value) => {
    return { age: `${value}`, value: randomInt(1, 100)() };
  })
);

const surgeryTypeColors = generateColors(Object.keys(typeOfSurgery.value));
const complicationColors = generateColors(Object.keys(surgicalComplications.value));
const surgicalInterventionColors = generateColors(Object.keys(surgicalInterventions.value))

function setTypeOfSurgery () {
  const types = Object.keys(typeOfSurgery.value);
  const data = types.map(type => [type, randomInt(1,100)()])
    .sort((current, next) => current[1] < next[1] ? 1 : -1);

  totalCases.value = data
    .map(row => row[1])
    .reduce((sum,value) => sum + value,0);
  typeOfSurgery.value = Object.fromEntries(data);
}

function setSurgicalComplications (total) {
  const types = Object.keys(surgicalComplications.value);
  let currentTotal = total;
  const data = types.map((type,i) => {
    const randomValue = i === types.length -1  ? currentTotal : randomInt(1, currentTotal)();
    const row = [type, randomValue];
    currentTotal -= randomValue;
    return row
  })
  .sort((current,next) => current[1] < next[1] ? 1 : -1);
  surgicalComplications.value = Object.fromEntries(data);
}


function updateSurgicalComplications (value) {
  const title = Object.keys(value);
  surgicalComplicationsTitle.value = `${title} surgical complications`;
  const total = value[Object.keys(value)];
  setSurgicalComplications(total);
}

function setSurgicalInterventions () {
  const types = Object.keys(surgicalInterventions.value);
  let currentTotal = totalCases.value;
  const data = types.map((type, i) => {
    const randomValue = i === types.length - 1
      ? currentTotal
      : randomInt(1, currentTotal)()
    const row = [type, randomValue];
    currentTotal -= randomValue;
    return row
  })
  surgicalInterventions.value = Object.fromEntries(data);
}


setTypeOfSurgery();
setSurgicalComplications(totalCases.value);
setSurgicalInterventions();
</script>
