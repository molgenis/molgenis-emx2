<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Surgical overview for all centers</h2>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="typeOfCraniosynostosis"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-affected-suture"
          title="Affected suture"
          :chartData="affectedSuture"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h3 class="dashboard-h3">Overview of all surgical interventions</h3>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-type-of-surgery"
          title="Type of surgery"
          :chartData="typeOfSurgery"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-complications"
          title="Complications"
          :chartData="complications"
          :chartColors="{
            Complications: '#426fab',
            'No complications': '#f3f4ff',
          }"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="cs-all-surgical-interventions"
          title="Surgical Interventions"
          :chartData="surgicalInterventions"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
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
          :chartHeight="250"
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
import { seq } from "../utils/devtools.js";

let ageAtSurgery = ref(
  seq(0, 14, 2).map((value) => {
    return { age: `${value}`, value: randomInt(1, 100)() };
  })
);

let typeOfCraniosynostosis = ref({
  "Non-syndromic": 35,
  Syndromic: 34,
  Familial: 16,
  Metabolic: 15,
});

let typeOfSurgery = ref({
  Vault: 29,
  Midface: 27,
  Hydrocephalus: 23,
  Aesthetic: 21,
});

let affectedSuture = ref({
  Sagittal: 30,
  Metopic: 20,
  Unicoronal: 18,
  Unilambdoid: 15,
  Frontosphenoidal: 10,
  Multiple: 7,
});

let complications = ref({
  Complications: 0.4,
  "No complications": 0.6,
});

let surgicalInterventions = ref({
  Single: 0.3,
  Unwanted: 0.4,
  Additional: 0.3,
});
</script>
