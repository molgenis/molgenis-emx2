<template>
  <Page id="page-dashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard v-else>
      <DashboardRow :columns="2">
        <DashboardChart>
          <!-- MAP -->
        </DashboardChart>
        <DashboardChart>
          <!-- PATIENTS ENROLLED -->
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <!-- SEX AT BIRTH CHART -->
        </DashboardChart>
        <DashboardChart>
          <!-- AGE AT INCLUSION -->
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import {
  Page,
  Dashboard,
  DashboardRow,
  DashboardChart,
  LoadingScreen,
} from "molgenis-viz";

let loading = ref(true);
let error = ref(null);
let organisations = ref([]);

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        city
        country
        latitude
        longitude
        providerInformation {
          providerIdentifier
          hasSubmittedData
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = response.Organisations.map((row) => {
    const status = row.providerInformation.hasSubmittedData
      ? "Data Submitted"
      : "No Data";
    return { ...row, hasSubmittedData: status };
  });
  organisations.value = data;
}

onMounted(() => {});
</script>
