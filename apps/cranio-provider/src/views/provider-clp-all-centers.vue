<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Overview "level 1" outcomes</h2>
    <DashboardBox class="mb-4">
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
    </DashboardBox>
    <h3 class="dashboard-h3">Overview of patients {{ ageGroupFilter }} years old (n={{ totalCases }})</h3>
    <DashboardChartLayout :columns="1">
      <DashboardBox
        id="clp-outcome-cleft-q"
        class="mb-4 mt-4"
        v-if="showCleftOutcomes"
      >
        <GroupedColumnChart
          chartId="clp-outcome-cleft-q-hcp-ern"
          title="Cleft-Q Outcomes"
          :chartData="cleftOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="colors"
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox
        id="clp-outcome-cleft-q"
        class="mb-4 mt-4"
        v-if="showIcsOutcomes"
      >
        <GroupedColumnChart
          chartId="clp-outcome-ics-hcp-ern"
          title="ICS Outcomes"
          :chartData="icsOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="10"
          :yTickValues="[0, 5, 10]"
          :columnFillPalette="colors"
          :chartHeight="250"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, GroupedColumnChart, InputLabel } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

// generate random data for display purposes
import { randomGroupDataset } from "../utils/devtools";
let cleftOutcomes = ref([]);
let icsOutcomes = ref([]);
let showCleftOutcomes = ref(false);
let showIcsOutcomes = ref(true);
let ageGroupFilter = ref('3-4');
let totalCases = ref(0);

const colors = { "Your Center": "#66c2a4", "ERN Average": "#9f6491" };

function setOutcomesData() {
  cleftOutcomes.value = randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Jaw", "Lip", "School", "Social", "Speech"],
    5, 100
  );

  icsOutcomes.value = randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Average total score", "Total score"],
    1, 10
  );
  
  if (showCleftOutcomes.value) {
    totalCases.value = cleftOutcomes.value
      .map(row => row.value)
      .reduce((sum,value) => sum + value, 0);
  } else {
    totalCases.value = icsOutcomes.value
      .map(row => row.value)
      .reduce((sum,value) => sum + value, 0);
  }
}

function onYearOfBirthFilter(event) {
  const ageGroup = event.target.value;
  ageGroupFilter.value = ageGroup;
  showCleftOutcomes.value = ["8-9", "10-12", "18+"].includes(ageGroup);
  showIcsOutcomes.value = ["3-4", "5-6"].includes(ageGroup);
  setOutcomesData();
}

setOutcomesData();
</script>
