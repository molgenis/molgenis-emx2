<template>
  <ProviderDashboard id="provider-overview" class="provider-visualizations">
    <DashboardBox id="provider-overview-welcome">
      <h2>Welcome, {{ user }}!</h2>
      <p>
        Welcome to {{ organization.name }}'s dashboard. These pages provide an
        overview of the patients you have submitted to relavent subregistries.
        On the current page, you will find a snapshot of your center as of
        today.
      </p>
    </DashboardBox>
    <DashboardBox
      id="provider-overview-patients-submitted"
      class="value-highlight"
    >
      <h2>
        <UserGroupIcon />
        <span>{{ totalPatientsSubmitted }} patients submitted</span>
      </h2>
      <p>
        Your center submitted on average of
        {{ avergagePatientsSubmitted }} patients per month.
      </p>
    </DashboardBox>
    <DashboardBox id="provider-overview-current-rank" class="value-highlight">
      <h2>
        <TrophyIcon />
        <span>{{ currentCenterRanking }} in overall submitted patients</span>
      </h2>
      <p>
        Patient's from your center account for {{ percentOfRegistry }}% of the
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
  </ProviderDashboard>
</template>

<script setup>
import { ref } from "vue";
import { UserGroupIcon, TrophyIcon } from "@heroicons/vue/24/outline";
import { DashboardBox, PieChart } from "molgenis-viz";
import ProviderDashboard from "../components/ProviderDashboard.vue";

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
  Math.round((totalPatientsSubmitted.value / 500) * 100)
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

<style lang="scss">
#provider-overview {
  grid-template-areas:
    "header header"
    "totalPatients providerRank"
    "pieSubregistry pieSexAtBirth";
}

#provider-overview-welcome {
  grid-area: header;
}

#provider-overview-patients-submitted {
  grid-area: totalPatients;
}

#provider-overview-current-rank {
  grid-area: providerRank;
}

#provider-overview-patients-by-workstream {
  grid-area: pieSubregistry;
}

#provider-overview-patients-by-sex-at-birth {
  grid-area: pieSexAtBirth;
}
</style>
