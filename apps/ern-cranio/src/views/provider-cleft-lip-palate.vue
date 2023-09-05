<template>
  <h2>Overview for your center</h2>
  <div id="clpCenterOverview" class="dashboard-content clp-visualisations">
    <div class="dashboard-section visualisation">
      <PieChart
        chartId="patientsByPhenotype"
        title="Patients by phenotype"
        :chartData="patientsByPhenotype"
        :chartHeight="225"
        :chartScale="0.95"
        :enableHoverEvents="false"
        :enableClicks="false"
        :asDonutChart="true"
      />
    </div>
    <div class="dashboard-section visualisation">
      <PieChart
        chartId="patientsByOutcome"
        title="Patients by phenotype"
        :chartData="patientsBySurgicalOutcome"
        :chartHeight="225"
        :chartScale="0.95"
        :enableHoverEvents="false"
        :enableClicks="false"
        :asDonutChart="true"
      />
    </div>
    <div class="dashboard-section">
    </div>
    <div class="dashboard-section">
    </div>
  </div>
  <h2>Surgical Overview for your center</h2>
  <h3>Lip closure overview</h3>
  <div id="lipClosureOverview" class="dashboard-content clp-visualisations">
    <div class="dashboard-section visualisation">
    </div>
    <div class="dashboard-section visualisation">
    </div>
    <div class="dashboard-section visualisation">
    </div>
    <div class="dashboard-section visualisation">
    </div>
  </div>
  <h3>Soft palate closure overview</h3>
  <div id="softPalateClosureOverview" class="dashboard-content clp-visualisations">
    <div class="dashboard-section visualisation">
    </div>
    <div class="dashboard-section visualisation">
    </div>
  </div>
  
</template>

<script setup>
import { ref } from "vue";
import { PieChart } from "molgenis-viz";

// generate random data for display purposes
let patientsByPhenotype = ref({
  CL: 40,
  CLA: 30,
  CP: 20,
  CLAP: 10,
})

let patientsBySurgicalOutcome = ref({
  'Lip Closure': 50,
  'Soft Palate': 15,
  'Hard Palate': 10,
  'Alveolar Bone Graft': 25,
})

</script>

<style lang="scss">
.clp-visualisations {
  grid-template-columns: 1fr 1fr;
}
</style>