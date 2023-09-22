<template>
  <ProviderDashboard>
    <h2>Overview for your center</h2>
    <DashboardChartLayout :columns="2">
      <DashboardBox id="clp-patients-by-phenotype">
        <PieChart2
          chartId="patientsByPhenotype"
          title="Patients by phenotype"
          :chartData="patientsByPhenotype"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox id="clp-patients-by-surgical-outcome">
        <PieChart2
          chartId="patientsByOutcome"
          title="Patients by surgical outcome"
          :chartData="patientsBySurgicalOutcome"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox id="clp-cleft-q-completed">
        <ProgressMeter
          chartId="cleftQCompleted"
          title="Cleft-Q Completed"
          :value="qCompleted"
          :totalValue="qTotal"
          :barHeight="25"
        />
      </DashboardBox>
      <DashboardBox id="clp-ics-completed">
        <ProgressMeter
          chartId="icsCompleted"
          title="ICS Completed"
          :value="icsCompleted"
          :totalValue="icsTotal"
          :barHeight="25"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h2>Surgical Overview for your center</h2>
    <h3>Lip closure overview</h3>
    <DashboardChartLayout>
      <DashboardBox id="clp-timing-of-lip-closure">
        <ColumnChart
          chartId="timingLipClosure"
          title="Timing of lip closure"
          :chartData="timingLipClosure"
          xvar="week"
          yvar="value"
          xAxisLabel="Weeks"
          yAxisLabel="Number of cases"
          :yMax="100"
          :yTickValues="[0, 20, 40, 60, 80, 100]"
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="lipClosureOutcomes"
          title="Lip closure surgical outcomes"
          :chartData="lipClosureOutcomes"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h3>Soft palate closure overview</h3>
    <DashboardChartLayout>
      <DashboardBox id="clp-soft-palate-closure">
        <ColumnChart
          chartId="softPalateClosure"
          title="Timing of soft palate closure"
          :chartData="softPalateClosure"
          xvar="week"
          yvar="value"
          xAxisLabel="Weeks"
          yAxisLabel="Number of cases"
          :yMax="100"
          :yTickValues="[0, 20, 40, 60, 80, 100]"
          :chartHeight="250"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="softPalateClosureOutcomes"
          title="Soft Palate surgical outcomes"
          :chartData="softPalateClosureOutcomes"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartHeight="200"
          :chartScale="0.9"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import {
  DashboardBox,
  PieChart2,
  ProgressMeter,
  ColumnChart,
} from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

// generate random data for display purposes
import { randomInt } from "d3";

let patientsByPhenotype = ref({
  CL: 40,
  CLA: 30,
  CP: 20,
  CLAP: 10,
});

let patientsBySurgicalOutcome = ref({
  "Lip Closure": 50,
  "Soft Palate": 15,
  "Hard Palate": 10,
  "Alveolar Bone Graft": 25,
});

let qCompleted = ref(randomInt(12, 31)());
let qTotal = ref(randomInt(32, 56)());

let icsCompleted = ref(randomInt(1, 41)());
let icsTotal = ref(randomInt(42, 96)());

let timingLipClosure = ref([
  { week: "8", value: randomInt(0, 100)() },
  { week: "9", value: randomInt(0, 100)() },
  { week: "10", value: randomInt(0, 100)() },
  { week: "11", value: randomInt(0, 100)() },
  { week: "12", value: randomInt(0, 100)() },
]);

let lipClosureOutcomes = ref({
  Complications: 20,
  Incidents: 20,
  Successful: 60,
});

let softPalateClosure = ref([
  { week: "8", value: randomInt(0, 100)() },
  { week: "9", value: randomInt(0, 100)() },
  { week: "10", value: randomInt(0, 100)() },
  { week: "11", value: randomInt(0, 100)() },
  { week: "12", value: randomInt(0, 100)() },
]);

let softPalateClosureOutcomes = ref({
  Complications: 50,
  Incidents: 10,
  Successful: 40,
});
</script>
