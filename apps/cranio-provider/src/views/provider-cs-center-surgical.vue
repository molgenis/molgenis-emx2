<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for your center</h2>
    <h3 class="dashboard-h3">Surgical complications</h3>
    <DashboardBox>
      <InputLabel
        id="surgeryTypeInput"
        label="Select a type of surgery"
      />
      <select id="surgeryTypeInput" @change="onSurgeryTypeInput">
        <option>Extracranial procedures</option>
        <option>Hydrocephalus</option>
        <option>Midface</option>
        <option>Vault</option>
      </select>
    </DashboardBox>
    <DashboardChartLayout :columns="1">
      <DashboardBox>
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
      </DashboardBox>
    </DashboardChartLayout>
    <h2 class="dashboard-h2">Surgical interventions by diagnosis</h2>
    <DashboardBox>
      <InputLabel
        id="diagnosisInput"
        label="Select a diagnosis"
      />
      <select id="diagnosisInput" @change="onDiagnosisInput">
        <option value=ORPHA:87>Apert syndrome</option>
        <option value=ORPHA:207>Crouzon syndrome</option>
        <option value=ORPHA:93262>Crouzon syndrome-acanthosis nigricans syndrome</option>
        <option value=OSNEW1>ERF-related craniosynostosis syndrome</option>
        <option value=ORPHA:53271>Muenke syndrome</option>
        <option value=ORPHA:3366>Non-syndromic metopic craniosynostosis</option>
        <option value=ORPHA:35093>Non-syndromic sagittal craniosynostosis</option>
        <option value=ORPHA:620102>Non-syndromic unicoronal craniosynostosis</option>
        <option value=ORPHA:620113>Non-syndromic unilambdoid craniosynostosis</option>
        <option value=ORPHA:794>Saethre-Chotzen syndrome</option>
        <option value=OSNEW5>TCF12-related craniosynostosis</option>
        <option value="ORPHA:620198">non-syndromic multistural craniosynostosis</option>
      </select>
    </DashboardBox>
    <div class="dashboard-chart-layout" id="surgicalInterventionsLayout">
      <DashboardBox>
        <GroupedColumnChart
          chartId="cs-center-surgical-interventions-combined"
          title="Surgical interventions"
          :chartData="interventionsByGroup"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="palette"
          :chartHeight="240"
        />
      </DashboardBox>
      <DashboardBox>
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
        />
      </DashboardBox>
    </div>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, GroupedColumnChart, InputLabel } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

import { randomGroupDataset, seq } from "../utils/devtools";

let complicationsByGroup = ref([]);
let interventionsByGroup = ref([]);
let ageByGroup = ref([]);

const palette = {
  'Your center': '#3f6597',
  ERN: '#66c2a4',
}

function setComplicationsByGroup () {
  complicationsByGroup.value = randomGroupDataset(
    ["Your center", "ERN"], ["Complications", "No complications"], 0, 100
  )
  .sort((a, b) => (a.group > b.group ? -1 : 1));
}

function setInterventionsByGroup () {
  interventionsByGroup.value = randomGroupDataset(
    ["Your center", "ERN"],
    ["Additional", "Unwanted"],
    0,
    100
  ).sort((a, b) => (a.group > b.group ? -1 : 1)) 
}

function setAgeByGroup () {
  ageByGroup.value = randomGroupDataset(["Your center", "ERN"], seq(0, 14, 1), 0, 18);
}

function onSurgeryTypeInput () {
  setComplicationsByGroup();
}

function onDiagnosisInput () {
  setInterventionsByGroup();
  setAgeByGroup();
}


setComplicationsByGroup();
setInterventionsByGroup();
setAgeByGroup();
</script>

<style lang="scss">
#surgicalInterventionsLayout {
  display: grid;
  grid-template-columns: 0.7fr 1.5fr;
}
</style>