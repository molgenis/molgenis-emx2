<template>
  <ProviderDashboard>
    <DashboardBox class="mb-4">
      <h2>General overview for your centers</h2>
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
      <DashboardBox>
        <PieChart
          chartId="cs-center-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="centerTypeOfCs"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-center-affect-suture"
          title="Affected Suture"
          :chartData="centerAffectedSuture"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-center-gentic-diagnosis"
          title="Genetic Diagnosis"
          :chartData="centerGeneticDiagnosis"
          :chartColors="{
            Available: '#426fab',
            'Not Available': '#f3f4ff'
          }"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import { DashboardBox, PieChart, InputLabel } from "molgenis-viz";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object
})


// define random data
let yearOfBirthOptions= ref(Array.from({length: 16}, (v,i) => 1980+i));

let centerTypeOfCs = ref({
  Syndromic: 45,
  'Non-syndromic': 20,
  Familial: 15,
  Metabolic: 13,
  Iatrogenic: 7
});

let centerAffectedSuture = ref({
  Sagittal: 35,
  Metopic: 14,
  Unicoronal: 13,
  Unilambdoid: 13,
  Frontosphenoidal: 13,
  Multiple: 12
});

let centerGeneticDiagnosis = ref({
  Available: 60,
  'Not Available': 40,
});


</script>