<template>
  <Page class="page-dashboard">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Dashboard"
      height="medium"
    />
    <PageSection
      id="section-intro-title"
      aria-labelledby="section-intro-title"
      :verticalPadding="2"
      v-if="error"
    >
      <MessageBox type="error">
        <p>Unable to retrieve data: {{ error }}</p>
      </MessageBox>
    </PageSection>
    <div class="dashboard" v-else>
      <div class="dashboard-box viz-table">
        <div class="dashboard-viz">
          <DataTable
            tableId="workstreamSummary"
            caption="Percentage of patients by workstream"
            :data="workstreamSummary"
            :columnOrder="['workstream', 'percent']"
          />
        </div>
      </div>
      <div class="dashboard-box viz-pie-chart">
        <div class="dashboard-viz">
          <PieChart
            chartId="sexAtBirth"
            :chartData="sexAtBirth"
            :chartHeight="225"
            title="Sex at birth"
          />
        </div>
      </div>
      <div class="dashboard-box viz-map">
        <div class="dashboard-viz">
          <GeoMercator
            chartId="dataProvidersMap"
            title="Data Providers"
            :geojson="geojson"
            :chartData="providers"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            groupingVariable="hasSubmittedData"
            :groupColorMappings="{
              'Data Submitted': '#f1681d',
              'No Data': '#f0f0f0'
            }"
            :tooltipTemplate="
              row => {
                return `
                <p class='title'>${row.projectName}</p>
                <p class='location'>${row.city}, ${row.country}</p>
              `;
              }
            "
            :enableLegendClicks="true"
            :legendData="{
              'Data Submitted': '#f1681d',
              'No Data': '#f0f0f0'
            }"
            :pointRadius="7"
            :mapColors="{
              water: '#43abcc',
              land: '#93dbab',
              border: '#1f171a'
            }"
            :mapCenter="{ latitude: 13, longitude: 50 }"
            :zoomLimits="[0.3, 10]"
          />
        </div>
      </div>
    </div>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  Page,
  PageHeader,
  PageSection,
  MessageBox,
  GeoMercator,
  DataTable,
  PieChart
} from "molgenis-viz";

import {
  fetchData,
  subsetData,
  renameKey,
  asDataObject
} from "$shared/utils/utils.js";

import geojson from "$shared/data/world.geo.json";

let loading = ref(true);
let error = ref(false);
let providers = ref([]);
let workstreamSummary = ref([]);
let sexAtBirth = ref([]);

const statsQuery = `
{
  Components {
    name
    statistics {
      id
      value
      label
      valueOrder
    }
  }
}
`

const providersQuery = `
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
`

function getDashboardData() {
  loading.value = true;
  Promise.all([
    fetchData(providersQuery),
    fetchData(statsQuery),
  ])
    .then(response => {
      const organisations = response[0].data.Organisations;
      providers.value = organisations.map(row => {
        return {
          ...row,
          hasSubmittedData: row.providerInformation[0].hasSubmittedData
            ? "Data Submitted"
            : "No Data"
        };
      });

      
      const stats = response[1].data.Components;
      const workstream = subsetData(stats,"name","patients-by-workstream")[0].statistics
        .map(row => {
          return { ...row, value: `${Math.round(parseFloat(row.value) * 100)}%` };
        });

      renameKey(workstream, "label", "workstream");
      renameKey(workstream, "value", "percent");
      workstreamSummary.value = workstream;

      const patientsSex = subsetData(
        stats,
        "name",
        "patients-sex-at-birth"
      )[0].statistics;
      sexAtBirth.value = asDataObject(patientsSex, "label", "value");
    })
    .then(() => {
      loading.value = false;
    })
    .catch(response => {
      const err = JSON.parse(response.message);
      error.value = `${err.message} (${err.status})`;
    });
}

onMounted(() => getDashboardData());
</script>
