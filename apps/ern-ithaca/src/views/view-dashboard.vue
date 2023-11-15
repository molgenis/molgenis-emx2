<template>
  <Page>
    <LoadingScreen v-if="loading" />
    <PageSection
      class="section-error"
      aria-labelledby="section-error-title"
      v-else-if="!loading && error"
    >
      <h2 id="section-error-title">Error!</h2>
      <MessageBox type="error">
        <p>Unable to load dashboard. {{ error }}</p>
      </MessageBox>
    </PageSection>
    <Dashboard id="publicDashboard" class="bg-blue-100" :verticalPadding="0" :horizontalPadding="3" v-else>
      <MessageBox type="warning">
        <p>
          The data displayed in the charts below was created for demonstration
          and testing purposes only. The actual data will be displayed at a
          later date.
        </p>
      </MessageBox>
      <DashboardRow :columns="2">
        <DashboardChart>
          <DataTable
            tableId="enrollment-by-subregistry"
            caption="Participant enrollment by subregistry"
            :data="casesBySubregistry"
            :columnOrder="['group', 'number of participants']"
          />
        </DashboardChart>
        <DashboardChart id="table-genes">
          <DataTable
            tableId="cases-by-gene"
            caption="Number of cases by gene"
            :data="casesByGene"
            :columnOrder="['gene', 'number of cases']"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="1">
        <DashboardChart id="provider-map" :verticalPadding="0" :horizontalPadding="0">
          <GeoMercator
            chartId="expert-centers-map"
            :geojson="WorldGeoJson"
            :chartData="providers"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            groupingVariable="definition"
            :groupColorMappings="mapColorPalette"
            :legendData="mapColorPalette"
            :mapCenter="{ latitude: 13, longitude: 50 }"
            :zoomLimits="[0.3, 10]"
            :mapColors="{
              land: '#709190',
              border: '#061428',
              water: '#061428',
            }"
            :tooltipTemplate="
              (row) => {
                return `
              <p class='title'>${row.name}</p>
              <p class='center-location'>
                <span class='location-city'>${row.city}</span>
                <span class='location-country'>${row.country}</span>
              </p>
              <p class='center-type'>${row.definition}</p>
              `;
              }
            "
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  Page,
  PageSection,
  Dashboard,
  DashboardRow,
  DashboardChart,
  MessageBox,
  LoadingScreen,
  DataTable,
  GeoMercator,
  WorldGeoJson,
  renameKey,
} from "molgenis-viz";

import gql from "graphql-tag";
import { request } from "graphql-request";

let loading = ref(true);
let error = ref(null);
let stats = ref([]);
let casesByGene = ref([]);
let casesBySubregistry = ref([]);
let providers = ref([]);

const mapColorPalette = {
  "Full member": "#F1FAEE",
  "Affiliated partner": "#FFA69E",
  Hub: "#843B62",
};

async function getStats() {
  const query = gql`
    {
      Components {
        name
        statistics {
          id
          label
          value
          valueOrder
        }
      }
    }
  `;

  const response = await request("../api/graphql", query);
  const data = await response.Components;
  return data;
}

async function getProviders() {
  const query = gql`
    {
      Organisations {
        name
        definition
        code
        city
        country
        latitude
        longitude
      }
    }
  `;

  const response = await request("../api/graphql", query);
  const data = await response.Organisations;
  return data;
}

async function loadData() {
  stats.value = await getStats();
  providers.value = await getProviders();
}

onMounted(() => {
  loadData()
    .then(() => {
      casesByGene.value = stats.value
        .filter((row) => row.name === "cases-by-gene")[0]
        .statistics.sort((current, next) =>
          current.valueOrder < next.valueOrder ? -1 : 1
        );
      renameKey(casesByGene.value, "label", "gene");
      renameKey(casesByGene.value, "value", "number of cases");

      casesBySubregistry.value = stats.value
        .filter((row) => row.name === "enrollment-by-subregistry")[0]
        .statistics.sort((current, next) =>
          current.valueOrder < next.valueOrder ? -1 : 1
        );
      renameKey(casesBySubregistry.value, "label", "group");
      renameKey(casesBySubregistry.value, "value", "number of participants");
    })
    .then(() => (loading.value = false))
    .catch((err) => {
      loading.value = false;
      error.value = err;
    });
});
</script>

<style lang="scss">
#publicDashboard {
  .d3-table {
    caption {
      @include setChartTitle;
    }
    
    td {
      padding: 0.5em 0.3em;
      font-size: 0.9em;
    }
  }
}
</style>