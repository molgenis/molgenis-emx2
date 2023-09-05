<template>
  <div id="providerOverview" class="dashboard-content">
    <div id="welcomeMessage" class="dashboard-section">
      <h2>Welcome, [user]!</h2>
      <p>Welcome to {{ center }}'s dashboard. These pages provide an overview of the patients you have submitted to relavent subregistries. On the current page, you will find a snapshot of your center as of today.</p>
    </div>
    <div id="totalPatientsSubmitted" class="dashboard-section value-highlight">
      <h2>
        <UserGroupIcon />
        <span>{{ totalPatientsSubmitted }} patients submitted</span>
      </h2>
      <p>Your center submitted on average of {{ avergagePatientsSubmitted }} patients per month.</p>
    </div>
    <div id="currentCenterRanking" class="dashboard-section value-highlight">
      <h2>
        <TrophyIcon />
        <span>{{ currentCenterRanking }} in overall submitted patients</span>
      </h2>
      <p>Patient's from your center account for {{ percentOfRegistry }}% of the registry.</p>
    </div>
    <div id="patientsBySubregistry" class="dashboard-section visualisation">
      <PieChart
        chartId="patientsByWorkstream"
        title="Patients submitted by workstream"
        :chartData="patientsBySubregistry"
        :chartHeight="225"
        :asDonutChart="true"
        :chartScale="0.9"
        :enableHoverEvents="false"
        :enableClicks="false"
      />
    </div>
    <div id="patientsBySexAtBirth" class="dashboard-section visualisation">
      <PieChart
        chartId="sexAtBirth"
        title="Sex at birth"
        :chartData="sexAtBirth"
        :chartHeight="225"
        :asDonutChart="true"
        :chartScale="0.9"
        :enableHoverEvents="false"
        :enableClicks="false"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { UserGroupIcon, TrophyIcon } from "@heroicons/vue/24/outline";
import { PieChart } from "molgenis-viz";

const route = useRoute();
const center = ref(route.params.provider);

// generate random data for display purposes
import { randomInt } from "d3";
let totalPatientsSubmitted = ref(randomInt(25,100)());
let avergagePatientsSubmitted = ref(Math.floor(totalPatientsSubmitted.value / 9));

const rank = randomInt(1, 25)();
let currentCenterRanking = ref(rank === 1 ? '1st' : (rank === 2 ? '2nd' : `${rank}th`));
let percentOfRegistry = ref(Math.round((totalPatientsSubmitted.value / 500) * 100));

let patientsBySubregistry = ref({
  'Cleft lip and palate': 0.4,
  Craniosynostosis: 0.3,
  Larynxcleft:  0.25,
  'Genetic Deafness': 0.05,
})

let sexAtBirth = ref({
  Female: 0.5,
  Male: 0.35,
  Undetermined: 0.15
})

</script>

<style lang="scss">
.value-highlight {
  h2 {
    font-size: 16pt;
    svg {
      width: 21px;
      margin-top: -2px;
      margin-right: 8px;
    }
  }
}

#providerOverview {
  grid-template-areas: 
    "header header"
    "totalPatients providerRank"
    "pieSubregistry pieSexAtBirth";
}

#welcomeMessage {
  grid-area: header;
}

#totalPatientsSubmitted {
  grid-area: totalPatients;
}

#currentCenterRanking {
  grid-area: providerRank;
}

#patientsBySubregistry {
  grid-area: pieSubregistry;
}

#patientsBySexAtBirth {
  grid-area: pieSexAtBirth;
}

</style>