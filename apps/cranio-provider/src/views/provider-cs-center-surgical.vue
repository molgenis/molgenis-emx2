<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for your center</h2>
    <h3 class="dashboard-h3">Surgical complications</h3>
    <DashboardRow :columns="1">
      <DashboardChart>
        <InputLabel id="surgeryTypeInput" label="Select a type of surgery" />
        <select id="surgeryTypeInput" @change="onSurgeryTypeInput">
          <option>Extracranial procedures</option>
          <option>Hydrocephalus</option>
          <option>Midface</option>
          <option>Vault</option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <GroupedColumnChart
          chartId="cs-center-surgical-complications-combined"
          title="Surgical complications"
          :chartData="complicationsByGroup"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="palette"
          :chartHeight="200"
        />
      </DashboardChart>
    </DashboardRow>
    <h2 class="dashboard-h2">Surgical interventions by diagnosis</h2>
    <DashboardRow :columns="2">
      <DashboardChart>
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
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <PieChart2
          chartId="cs-center-surgical-interventions"
          title="Surgical Interventions"
          :chartData="surgicalInterventions"
          :chartColors="surgicalInterventionColors"
          :asDonutChart="true"
          :enableLegendHovering="true"
          :stackLegend="true"
          legendPosition="right"
          :chartHeight="200"
          :chartScale="0.4"
          :valuesArePercents="false"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <GroupedColumnChart
          chartId="cs-center-age-at-first-surgery-combined"
          title="Age at first surgery"
          :chartData="ageByGroup"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="20"
          xAxisLabel="Age (months)"
          :yTickValues="[0, 5, 10, 15, 20]"
          :columnPaddingInner="0.2"
          :columnPaddingOuter="0.3"
          :columnFillPalette="palette"
          :chartHeight="225"
          :chartMargins="{ top: 10, right: 0, bottom: 60, left: 30 }"
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
  GroupedColumnChart,
  InputLabel,
  PieChart2,
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// create random datasets for demo purposes
import { randomInt } from "d3";
import generateColors from "../utils/palette.js";
import { randomGroupDataset, seq } from "../utils/devtools";

let totalCases = ref(0);
let complicationsByGroup = ref([]);
let ageByGroup = ref([]);
let surgicalInterventions = ref({
  "Additional planned surgery according to protocol": 0,
  "First surgery": 0,
  "Unwanted reoperation due to complications": 0,
});

const surgicalInterventionColors = generateColors(
  Object.keys(surgicalInterventions.value)
);

const palette = {
  "Your center": "#3f6597",
  ERN: "#66c2a4",
};

function setComplicationsByGroup() {
  complicationsByGroup.value = randomGroupDataset(
    ["Your center", "ERN"],
    ["Complications", "No complications"],
    0,
    100
  ).sort((a, b) => (a.group > b.group ? -1 : 1));

  totalCases.value = complicationsByGroup.value
    .map((row) => row.value)
    .reduce((sum, value) => sum + value, 0);
}

function setSurgicalInterventions() {
  const types = Object.keys(surgicalInterventions.value);
  let currentTotal = totalCases.value;
  const data = types
    .map((type, i) => {
      const randomValue =
        i === types.length - 1
          ? currentTotal
          : i === 1
          ? Math.round(currentTotal * 0.45)
          : randomInt(1, currentTotal)();
      const row = [type, randomValue];
      currentTotal -= randomValue;
      return row;
    })
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  surgicalInterventions.value = Object.fromEntries(data);
}

function setAgeByGroup() {
  ageByGroup.value = randomGroupDataset(
    ["Your center", "ERN"],
    seq(0, 14, 1),
    0,
    18
  );
}

function onSurgeryTypeInput() {
  setComplicationsByGroup();
}

function onDiagnosisInput() {
  setSurgicalInterventions();
  setAgeByGroup();
}

setComplicationsByGroup();
setSurgicalInterventions();
setAgeByGroup();
</script>
