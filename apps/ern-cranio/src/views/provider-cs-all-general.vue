<template>
  <ProviderDashboard>
    <DashboardBox class="mb-4">
      <h2>General overview for all centers</h2>
      <p>The following charts provides a general overview for all centers.</p>
      <InputLabel
        id="yearOfBirthFilter"
        label="Year of birth"
        description="Limit the results by year of birth"
      />
      <select class="inputs select" id="yearOfBirthFilter">
        <option value="all">All Patients</option>
        <option v-for="year in yearOfBirthOptions" :value="year">
          {{ year }}
        </option>
      </select>
    </DashboardBox>
    <DashboardChartLayout :columns="2">
      <DashboardBox id="type-of-craniosynostosis">
        <PieChart2
          chartId="cs-types"
          title="Type of craniosynostosis"
          :chartData="csTypes"
          :chartHeight="200"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="cs-affected-sutures"
          title="Affected Suture"
          :chartData="affectedSuture"
          :chartHeight="200"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.9"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart2
          chartId="geneticDiagnosisAvailability"
          title="Genetic Diagnosis"
          :chartData="geneticDiagnosis"
          :chartColors="{
            Available: '#426fab',
            'Not Available': '#f3f4ff',
          }"
          :chartHeight="200"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
        />
      </DashboardBox>
      <DashboardBox>
        <ColumnChart
          chartId="countryOfResidence"
          title="Patients by country of residence"
          :chartData="countryOfResidence"
          xvar="country"
          yvar="value"
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
import { DashboardBox, PieChart2, InputLabel, ColumnChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

import { randomInt } from "d3";

let yearOfBirthOptions = ref(Array.from({ length: 16 }, (v, i) => 1980 + i));

let csTypes = ref({
  Syndromic: 45,
  "Non-syndromic": 20,
  Familial: 15,
  Metabolic: 13,
  Iatrogenic: 7,
});

let affectedSuture = ref({
  Sagittal: 35,
  Metopic: 14,
  Unicoronal: 13,
  Unilambdoid: 13,
  Frontosphenoidal: 13,
  Multiple: 12,
});

let geneticDiagnosis = ref({
  Available: 60,
  "Not Available": 40,
});

let countryOfResidence = ref([
  { country: "NL", value: randomInt(10, 100)() },
  { country: "FR", value: randomInt(10, 100)() },
  { country: "GE", value: randomInt(10, 100)() },
  { country: "ES", value: randomInt(10, 100)() },
  { country: "PO", value: randomInt(10, 100)() },
  { country: "SE", value: randomInt(10, 100)() },
]);
</script>
