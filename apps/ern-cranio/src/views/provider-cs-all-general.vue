<template>
  <ProviderDashboard class="two-column-layout">
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
    <div class="provider-visualizations">
      <DashboardBox id="type-of-craniosynostosis">
        <PieChart
          chartId="cs-types"
          title="Type of craniosynostosis"
          :chartData="csTypes"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-affected-sutures"
          title="Affected Suture"
          :chartData="affectedSuture"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="geneticDiagnosisAvailability"
          title="Genetic Diagnosis"
          :chartData="geneticDiagnosis"
          :chartColors="{
            Available: '#426fab',
            'Not Available': '#f3f4ff'
          }"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
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
          :yTickValues="[0,25,50,75,100]"
          :chartHeight="250"
        />
      </DashboardBox>
    </div>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { DashboardBox, PieChart, InputLabel, ColumnChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object
})

import { randomInt } from "d3";

let yearOfBirthOptions= ref(Array.from({length: 16}, (v,i) => 1980+i));

let csTypes = ref({
  Syndromic: 45,
  'Non-syndromic': 20,
  Familial: 15,
  Metabolic: 13,
  Iatrogenic: 7
});

let affectedSuture = ref({
  Sagittal: 35,
  Metopic: 14,
  Unicoronal: 13,
  Unilambdoid: 13,
  Frontosphenoidal: 13,
  Multiple: 12
});


let geneticDiagnosis = ref({
  Available: 60,
  'Not Available': 40,
});

let countryOfResidence = ref([
  { country: 'NL', value: randomInt(10, 100)()},
  { country: 'FR', value: randomInt(10, 100)()},
  { country: 'GE', value: randomInt(10, 100)()},
  { country: 'ES', value: randomInt(10, 100)()},
  { country: 'PO', value: randomInt(10, 100)()},
  { country: 'SE', value: randomInt(10, 100)()},
]);

</script>