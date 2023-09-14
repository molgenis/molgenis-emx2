<template>
  <ProviderDashboard>
    <DashboardBox id="provider-overview-welcome" class="mb-4">
      <h2>Welcome, {{ user }}!</h2>
      <p>
        Welcome to {{ organization.name }}'s dashboard. These pages provide an
        overview of the patients you have submitted to relavent subregistries.
        On the current page, you will find a snapshot of your center as of
        today.
      </p>
    </DashboardBox>
    <DashboardChartLayout :columns="2">
      <DashboardBox
        id="provider-overview-patients-submitted"
        class="value-highlight"
      >
        <h2>
          <UserGroupIcon />
          <span>{{ totalPatientsSubmitted }} patients submitted</span>
        </h2>
        <p>
          Your center submitted on average
          {{ avergagePatientsSubmitted }} patients per month.
        </p>
      </DashboardBox>
      <DashboardBox id="provider-overview-current-rank" class="value-highlight">
        <h2>
          <TrophyIcon />
          <span>{{ currentCenterRanking }} in overall submitted patients</span>
        </h2>
        <p>
          Patients from your center account for {{ percentOfRegistry }} of the
          registry.
        </p>
      </DashboardBox>
      <DashboardBox id="provider-overview-patients-by-workstream">
        <PieChart
          chartId="patientsByWorkstream"
          title="Patients submitted by workstream"
          :chartData="patientsBySubregistry"
          :chartHeight="pieChartHeight"
          :asDonutChart="true"
          :chartScale="0.95"
        />
      </DashboardBox>
      <DashboardBox id="provider-overview-patients-by-sex-at-birth">
        <PieChart
          chartId="sexAtBirth"
          title="Sex at birth"
          :chartData="sexAtBirth"
          :chartHeight="pieChartHeight"
          :asDonutChart="true"
          :chartScale="0.95"
        />
      </DashboardBox>
    </DashboardChartLayout>
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { UserGroupIcon, TrophyIcon } from "@heroicons/vue/24/outline";
import { DashboardBox, PieChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";
import DashboardChartLayout from "../components/DashboardChartLayout.vue";

const props = defineProps({
  user: String,
  organization: Object,
});

let pieChartHeight = ref(250);

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
  Math.round((totalPatientsSubmitted.value / 500) * 100) + '%'
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
