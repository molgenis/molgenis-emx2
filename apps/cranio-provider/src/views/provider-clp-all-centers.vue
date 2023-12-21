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
    <DashboardRow :columns="1">
      <DashboardChart id="clp-outcome-cleft-q" v-if="showCleftOutcomes">
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
      </DashboardChart>
      <DashboardChart id="clp-outcome-cleft-q" v-if="showIcsOutcomes">
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
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

// generate random data for display purposes
import { randomGroupDataset } from "../utils/devtools";
let cleftOutcomes = ref([]);
let icsOutcomes = ref([]);
let showCleftOutcomes = ref(false);
let showIcsOutcomes = ref(true);
let ageGroupFilter = ref("3-4");
let totalCases = ref(0);

const colors = { "Your Center": "#66c2a4", "ERN Average": "#9f6491" };

function setOutcomesData() {
  cleftOutcomes.value = randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Jaw", "Lip", "School", "Social", "Speech"],
    5,
    100
  );

  icsOutcomes.value = randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Average total score", "Total score"],
    1,
    10
  );

  if (showCleftOutcomes.value) {
    totalCases.value = cleftOutcomes.value
      .map((row) => row.value)
      .reduce((sum, value) => sum + value, 0);
  } else {
    totalCases.value = icsOutcomes.value
      .map((row) => row.value)
      .reduce((sum, value) => sum + value, 0);
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
