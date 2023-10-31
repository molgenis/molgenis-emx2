<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Overview "level 1" outcomes</h2>
    <DashboardChartLayout :columns="2">
      <DashboardBox id="clp-outcome-cleft-q" class="mb-4 mt-4">
        <GroupedColumnChart
          chartId="clp-outcome-cleft-q-hcp-ern"
          title="Cleft-Q Outcomes after treatment"
          :chartData="cleftOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="{
            'Your Center': '#b2e2e2',
            'ERN Average': '#66c2a4',
          }"
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox id="clp-outcome-cleft-q" class="mb-4 mt-4">
        <GroupedColumnChart
          chartId="clp-outcome-ics-hcp-ern"
          title="ICS Outcomes after treatment"
          :chartData="icsOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="{
            'Your Center': '#b2e2e2',
            'ERN Average': '#66c2a4',
          }"
          :chartHeight="250"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h2 class="dashboard-h2">Overview of speech outcomes</h2>
    <DashboardChartLayout :columns="2">
      <DashboardBox class="mt-4 mb-4">
        <GroupedColumnChart
          chartId="clp-outcome-speech-pcc"
          title="PCC Scores after treatment"
          :chartData="speechPccOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="20"
          xAxisLabel="Age (yrs)"
          :yTickValues="[0, 5, 10, 15, 20]"
          :columnFillPalette="{
            'Your Center': '#b2e2e2',
            'ERN Average': '#66c2a4',
          }"
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox class="mt-4 mb-4">
        <GroupedColumnChart
          chartId="clp-outcome-speech-vpc"
          title="VPC Scores after treatment"
          :chartData="speechVpcOutcomes"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="20"
          xAxisLabel="Age (yrs)"
          :yTickValues="[0, 5, 10, 15, 20]"
          :columnFillPalette="{
            'Your Center': '#b2e2e2',
            'ERN Average': '#66c2a4',
          }"
          :chartHeight="250"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, GroupedColumnChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// generate random data for display purposes
import { randomGroupDataset } from "../utils/devtools";

let cleftOutcomes = ref(
  randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Jaw", "Lip", "School", "Social", "Speech"],
    1,
    100
  )
);

let icsOutcomes = ref(
  randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["Jaw", "Lip", "School", "Social", "Speech"],
    1,
    100
  )
);

let speechPccOutcomes = ref(
  randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["3", "5-6", "8-12", "18+"],
    1,
    20
  )
);

let speechVpcOutcomes = ref(
  randomGroupDataset(
    ["Your Center", "ERN Average"],
    ["3", "5-6", "8-12", "18+"],
    1,
    20
  )
);
</script>

<style lang="scss">
#provider-clp {
  .provider-visualizations {
    grid-template-columns: 1fr 1fr;
    margin-bottom: 2em;
  }
}
</style>
