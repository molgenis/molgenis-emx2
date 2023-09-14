<template>
  <ProviderDashboard>
    <h2>Surgical overview for all centers</h2>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart
          chartId="cs-all-surgical-type-of-craniosynostosis"
          title="Type of craniosynostosis"
          :chartData="typeOfCraniosynostosis"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-all-surgical-affected-suture"
          title="Affected suture"
          :chartData="affectedSuture"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
    </DashboardChartLayout>
    <h3>Overview of all surgical interventions</h3>
    <DashboardChartLayout>
      <DashboardBox>
        <PieChart
          chartId="cs-all-surgical-type-of-surgery"
          title="Type of surgery"
          :chartData="typeOfSurgery"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox>
        <PieChart
          chartId="cs-all-surgical-complications"
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
      <DashboardBox>
        <PieChart
          chartId="cs-all-surgical-interventions"
          title="Surgical Interventions"
          :chartData="surgicalInterventions"
          :chartHeight="225"
          :chartScale="0.95"
          :asDonutChart="true"
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
          :yTickValues="[0,25,50,75,100]"
          :chartHeight="250"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { DashboardBox, PieChart, ColumnChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object
})


// create random datasets for demo purposes
import { randomInt } from "d3";
import { randomPercentages } from "../utils/devtools.js";

function seq (start, stop, by) {
  return Array.from({ length: (stop - start) / by + 1 }, (_, i) => start + i * by);
}

let typeOfSurgery = ref({});
let complications = ref({});
let surgicalInterventions = ref({});
let ageAtSurgery = ref(seq(0, 14, 2).map(value => {
    return {age: `${value}`, value: randomInt(1,100)()}
}));

function generateChartData() {
  
  // generate datasets (objects) for pie charts
  
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
  
  // generate list of arrays for column chart
  ageAtSurgery.value = seq(0, 14, 2).map(value => {
    return {age: `${value}`, value: randomInt(1,100)()}
  });
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
  generateChartData();
});


function onClick() {
  generateChartData();
}

</script>