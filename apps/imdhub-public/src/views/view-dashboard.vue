<template>
  <Page>
    <PageHeader
      title="Recon4imd: IMD-hub"
      subtitle="Participant recruitment dashboard"
      imageSrc="recon4imd_header.jpg"
      height="medium"
    />
    <div v-if="!loading && error">
      <MessageBox type="error">
        <p><{{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      v-else
      :vertical-padding="0"
      :horizontal-padding="2"
      class="bg-blue-050"
    >
      <MessageBox type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
      <DashboardRow :columns="1">
        <DashboardChart>
          <GeoMercator
            chartId="organisations-map"
            title="Recon4imd Clinical Sites"
            description="The map below shows the location of all sites participating in the Recon4imd project."
            :geojson="WorldGeoJson"
            :chartData="organisations"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            :tooltipTemplate="
              (row) => {
                return `<p class='title'>${row.name}</p><p class='location'>${row.city}, ${row.country}</p>`;
              }
            "
            :enableLegendClicks="true"
            :pointRadius="7"
            :mapColors="{
              land: '#709190',
              border: '#061428',
              water: '#061428',
            }"
            :mapCenter="{ latitude: 13, longitude: 50 }"
            :zoomLimits="[0.5, 10]"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import {
  Page,
  PageHeader,
  PageSection,
  MessageBox,
  Dashboard,
  DashboardRow,
  DashboardChart,
  GeoMercator,
  WorldGeoJson,
} from "molgenis-viz";

import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import type { RouterViewIF, OrganisationsRecordIF } from "../interfaces";

const props = defineProps<RouterViewIF>();
const loading = ref<boolean>(true);
const error = ref<Error | null>(null);

const organisations = ref<OrganisationsRecordIF[]>([]);

async function getData() {
  const query = gql`
    {
      Organisations {
        name
        code
        city
        country
        latitude
        longitude
      }
    }
  `;

  const response = await request("../api/graphql", query);
  organisations.value = response.Organisations;
}

onMounted(() => {
  getData()
    .catch((err: Error) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
