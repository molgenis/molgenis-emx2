<template>
  <ProviderDashboard class="two-column-layout">
    <h2>Surgical overview for all centers</h2>
    <div class="provider-visualizations">
      <DashboardBox>
        <PieChart
          chartId="cs-surgical-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="typeOfCraniosynostosis"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-surgical-affected-suture"
          title="Affected suture"
          :chartData="affectedSuture"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
    </div>
    <h3>Overview of all surgical interventions</h3>
    <div class="provider-visualizations">
      <DashboardBox>
        <PieChart
          chartId="cs-surgical-type-of-surgery"
          title="Type of surgery"
          :chartData="typeOfSurgery"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
          
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-surgical-complications"
          title="Complications"
          :chartData="complications"
          :chartColors="{
            'Complications': '#426fab',
            'No complications': '#f3f4ff'
          }"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
    </div>
  </ProviderDashboard>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { DashboardBox, PieChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object
})


// create random datasets for demo purposes
import { randomPercentages } from "../utils/devtools.js";

let typeOfSurgery = ref({});
let complications = ref({});
let surgicalInterventions = ref({})

function generatePieData() {
  typeOfSurgery.value = randomPercentages({
    labels: ['Vault', "Midface", 'Hydrocephalus', 'Aesthetic'],
    asObject: true
  })
  
  complications.value = randomPercentages({
    labels: ['Complications', 'No complications'],
    asObject: true
  });
  
  surgicalInterventions.value = randomPercentages({
    labels: ['Single', 'Unwanted', 'Additional'],
    asObject: true
  })
  
}

let typeOfCraniosynostosis = ref(
  randomPercentages({
    labels: ['Non-syndromic', 'Syndromic', 'Familial', 'Metabolic'],
    asObject: true
  })
);

let affectedSuture = ref(
  randomPercentages({
    labels: ['Sagittal','Metopic', 'Unicoronal', 'Unilambdoid', 'Frontosphenoidal', 'Multiple'],
    asObject: true
  })
)

onMounted(() => {
  generatePieData();
});


function onClick() {
  generatePieData();
}

</script>