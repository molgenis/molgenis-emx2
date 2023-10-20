<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for your center</h2>
    <DashboardChartLayout :columns="2">
      <DashboardBox>
        <GroupedColumnChart
          chartId="cs-center-surgical-complications-combined"
          title="Surgical complications"
          description="Complications that occurred at your center and all centers combined"
          :chartData="combinedComplication"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="{
            'Your center': '#b2e2e2',
            ERN: '#66c2a4',
          }"
          :chartHeight="300"
        />
      </DashboardBox>
      <DashboardBox>
        <GroupedColumnChart
          chartId="cs-center-surgical-interventions-combined"
          title="Surgical interventions"
          description="Number of surgical interventions per patient for your center and all centers combined"
          :chartData="combinedSurgicalInterventions"
          group="group"
          xvar="category"
          yvar="value"
          :yMax="100"
          :yTickValues="[0, 25, 50, 75, 100]"
          :columnFillPalette="{
            'Your center': '#b2e2e2',
            ERN: '#66c2a4',
          }"
          :chartHeight="300"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <DashboardBox>
      <GroupedColumnChart
        chartId="cs-center-age-at-first-surgery-combined"
        title="Age at first surgery"
        :chartData="combinedAgeAtSurgery"
        group="group"
        xvar="category"
        yvar="value"
        :yMax="100"
        xAxisLabel="Age (months)"
        :yTickValues="[0, 25, 50, 75, 100]"
        :columnPaddingInner="0.2"
        :columnPaddingOuter="0.3"
        :columnFillPalette="{
          'Your center': '#b2e2e2',
          ERN: '#66c2a4',
        }"
        :chartHeight="325"
      />
    </DashboardBox>
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

import { randomGroupDataset, seq } from "../utils/devtools";

let combinedComplication = ref(
  randomGroupDataset(
    ["Your center", "ERN"],
    ["Complications", "No complications"],
    0,
    100
  ).sort((a, b) => (a.group > b.group ? -1 : 1))
);

let combinedSurgicalInterventions = ref(
  randomGroupDataset(
    ["Your center", "ERN"],
    ["Additional", "Unwanted"],
    0,
    100
  ).sort((a, b) => (a.group > b.group ? -1 : 1))
);

let combinedAgeAtSurgery = ref(
  randomGroupDataset(["Your center", "ERN"], seq(0, 14, 1), 0, 100)
);
</script>
