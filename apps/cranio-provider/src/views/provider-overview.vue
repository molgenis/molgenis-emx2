<template>
  <ProviderDashboard>
    <DashboardBox id="provider-overview-welcome" class="mb-4">
      <h2 class="dashboard-title">
        Welcome to <span>{{ providerName }}'s</span> dashboard!
      </h2>
      <p>
        Pages are grouped by workstream. You can view an overview of patients
        your centre has submitted to the ERN Cranio registry, and you can
        compare the results of your centre against the entire registry. On the
        current page, you will find a snapshot of your centre as of today.
      </p>
    </DashboardBox>
    <DashboardChartLayout :columns="2">
      <DashboardBox
        id="provider-overview-patients-submitted"
        class="center-showcase"
      >
        <ValueShowcase
          :title="`${totPatientsSubmitted} patients submitted`"
          :description="`Your center has submitted on average ${avgPatientsSubmitted} patients per month`"
        >
          <UserGroupIcon />
        </ValueShowcase>
      </DashboardBox>
    </DashboardChartLayout>
    <DashboardChartLayout :columns="2">
      <DashboardBox id="provider-overview-patients-by-workstream">
        <PieChart2
          chartId="patientsByWorkstream"
          title="Patients by workstream"
          :chartData="numPatientsByWorkstream"
          :chartHeight="215"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
          :valuesArePercents="false"
          :enableClicks="true"
          @slice-clicked="updateSelection"
        />
      </DashboardBox>
      <DashboardBox id="provider-overview-patients-by-sex-at-birth" v-if="showSexAtBirth">
        <PieChart2
          chartId="sexAtBirth"
          :title="sexAtBirthTitle"
          :chartData="sexAtBirth"
          :chartHeight="215"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
          :valuesArePercents="false"
        />
      </DashboardBox>
    </DashboardChartLayout> 
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { UserGroupIcon } from "@heroicons/vue/24/outline";
import { DashboardBox, PieChart2 } from "molgenis-viz";

import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";
import ValueShowcase from "../components/ValueShowcase.vue";

import viewProps from "../data/props";
const props = defineProps(viewProps);

import { randomInt } from "d3";

// generate random data for display purposes
let totPatientsSubmitted = ref(0);
let avgPatientsSubmitted = ref(0);
let numPatientsByWorkstream = ref({});
let showSexAtBirth = ref(false);
let sexAtBirth = ref({});
let sexAtBirthTitle = ref(null);
let patientsSexByWorkstream = ref([]);

function generatePatients () {
  const workstreams = ['Cleft lip and palate','Craniosynostosis','Genetic Deafness','Larynxcleft'];
  const sexAtBirthGroups = ['Female', "Male", 'Undetermined'];
  const patientsByWorkstream = workstreams.map(value => [value, randomInt(25,225)()]);
  
  numPatientsByWorkstream.value = Object.fromEntries(patientsByWorkstream.sort((a,b) => a[1] < b[1] ? 1 : -1));
  totPatientsSubmitted.value = patientsByWorkstream
    .map(arr => arr[1])
    .reduce((sum, value) => sum + value, 0);
    
  avgPatientsSubmitted.value = Math.round(totPatientsSubmitted.value / 12);
  
  patientsSexByWorkstream.value = workstreams.map(workstream => {
    let currentTotal = numPatientsByWorkstream.value[workstream];
    const sexByWorkstream = sexAtBirthGroups.map((group, i) => {
      const randomValue = i === sexAtBirthGroups.length-1 ? currentTotal : randomInt(1, currentTotal)();
      const row = [group,randomValue];
      currentTotal -= randomValue;
      return row
    })
    return {
      workstream: workstream,
      data: Object.fromEntries(sexByWorkstream.sort((a,b) => a[1] < b[1] ? 1 : -1))
    };
  })
}

generatePatients()

function updateSelection(value) {
  const workstream = Object.keys(value)[0];
  sexAtBirthTitle.value = `Sex at birth for ${workstream} patients`;  
  sexAtBirth.value = patientsSexByWorkstream.value.filter(row => row.workstream===workstream)[0].data;
  showSexAtBirth.value = true;
}

</script>
