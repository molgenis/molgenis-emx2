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
          :title="`${totalPatientsSubmitted} patients submitted`"
          :description="`Your center has submitted on average ${avergagePatientsSubmitted} patients per month`"
        >
          <UserGroupIcon />
        </ValueShowcase>
      </DashboardBox>
      <DashboardBox id="provider-overview-current-rank" class="center-showcase">
        <ValueShowcase
          :title="`${currentCenterRanking} in overall submitted patients`"
          :description="`Patients from your center account for ${percentOfRegistry} of the
          registry.`"
        >
          <TrophyIcon />
        </ValueShowcase>
      </DashboardBox>
      <DashboardBox id="provider-overview-patients-by-workstream">
        <PieChart2
          chartId="patientsByWorkstream"
          title="Patients by workstream"
          :chartData="patientsBySubregistry"
          :chartHeight="215"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
        />
      </DashboardBox>
      <DashboardBox id="provider-overview-patients-by-sex-at-birth">
        <PieChart2
          chartId="sexAtBirth"
          title="Sex at birth"
          :chartData="sexAtBirth"
          :chartHeight="215"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :chartScale="0.85"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { UserGroupIcon, TrophyIcon } from "@heroicons/vue/24/outline";
import { DashboardBox, PieChart2 } from "molgenis-viz";

import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";
import ValueShowcase from "../components/ValueShowcase.vue";

import viewProps from "../data/props";
const props = defineProps(viewProps);

import { randomInt } from "d3";

// generate random data for display purposes
let totalPatientsSubmitted = ref(randomInt(25, 100)());
let avergagePatientsSubmitted = ref(
  Math.floor(totalPatientsSubmitted.value / 9)
);

const rank = randomInt(1, 25)();
let currentCenterRanking = ref(
  rank === 1 ? "1st" : rank === 2 ? "2nd" : `${rank}th`
);
let percentOfRegistry = ref(
  Math.round((totalPatientsSubmitted.value / 500) * 100) + "%"
);

let patientsBySubregistry = ref({
  Larynxcleft: 56,
  "Cleft lip and palate": 21,
  Craniosynostosis: 14,
  "Genetic Deafness": 9,
});

let sexAtBirth = ref({
  Female: 50,
  Male: 35,
  Undetermined: 15,
});
</script>
