<template>
  <Page id="page-dashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data. {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard :verticalPadding="0" :horizontalPadding="2" v-else>
      <DashboardRow :columns="1">
        <DataValueHighlights
          title="ERN Skin at a glance"
          :data="registryHighlights"
        />
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <GeoMercator
            chartId="ernSkinOrganisationsMap"
            title="Status of data by healthcare provider"
            :geojson="WorldGeoJson"
            :chartData="organisations"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            groupingVariable="hasSubmittedData"
            :mapCenter="{
              latitude: 3,
              longitude: 51,
            }"
            :pointRadius="6"
            :groupColorMappings="{
              'Not Submitted': '#F1FAEE',
              Submitted: '#FFA69E',
            }"
            :legendData="{
              'Not Submitted': '#F1FAEE',
              Submitted: '#FFA69E',
            }"
            :mapColors="{
              land: '#709190',
              border: '#061428',
              water: '#061428',
            }"
            :tooltipTemplate="
              (row) => {
                return `
              <p class='title'>
                ${row.name}
              </p>
              <p class='center-info'>
                ${row.providerIdentifier}
              </p>
              <p class='center-location'>
                <span class='location-city'>${row.city}</span>
                <span class='location-country'>${row.country}</span>
              </p>
              `;
              }
            "
            :zoomLimits="[0.3, 10]"
            :enableLegendClicks="true"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            tableId="registryPatientsByGroup"
            caption=" Summary of patients enrolled by thematic disease group"
            :data="patientsByGroup"
            :columnOrder="['thematic disease group', 'patients']"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <ColumnChart
            chartId="registryPatientsByAgeGroup"
            title="Number of patients by age category"
            :chartData="ageByGroup"
            xvar="category"
            yvar="value"
            xAxisLineBreaker=";"
            :yMax="ageByGroupMax"
            :yTickValues="ageByGroupTicks"
            :chartHeight="225"
            :chartMargins="{ top: 20, right: 0, bottom: 40, left: 40 }"
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="registryPatientsBySexAtBirth"
            title="Sex at birth"
            :chartData="sexAtBirth"
            legendPosition="bottom"
            :chartHeight="150"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="10"
          />
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
  MessageBox,
  GeoMercator,
  DataValueHighlights,
  DataTable,
  PieChart2,
  ColumnChart,
  WorldGeoJson,
} from "molgenis-viz";

import { seqAlong } from "../utils/utils";
import { max } from "d3";
const d3 = { max };

let loading = ref(true);
let error = ref(null);
let registryHighlights = ref({});
let organisations = ref([]);
let ageByGroup = ref([]);
let ageByGroupMax = ref(0);
let ageByGroupTicks = ref([]);
let patientsByGroup = ref([]);
let sexAtBirth = ref([]);

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        code
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
  organisations.value = response.Organisations.map((row) => {
    return {
      ...row,
      hasSubmittedData: row.providerInformation[0].hasSubmittedData
        ? "Submitted"
        : "Not Submitted",
      providerIdentifier: row.providerInformation[0].providerIdentifier,
    };
  });
}

async function getStatistics() {
  const query = gql`
    {
      Components {
        name
        statistics {
          label
          value
          valueOrder
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = response.Components;

  const highlights = data
    .filter((row) => row.name === "highlights")[0]
    ["statistics"].map((row) => [row.label, row.value]);
  registryHighlights.value = Object.fromEntries(highlights);

  patientsByGroup.value = data
    .filter((row) => row.name === "enrolment")[0]
    ["statistics"].map((row) => {
      return {
        ...row,
        "thematic disease group": row.label,
        patients: row.value,
      };
    });

  ageByGroup.value = data
    .filter((row) => row.name === "age")[0]
    ["statistics"].map((row) => {
      return {
        ...row,
        category: `${row.label};${row.description}`,
      };
    });

  ageByGroupMax.value = Math.round(d3.max(ageByGroup.value, row => row.value) / 10 ) * 10;
  ageByGroupTicks.value = seqAlong(0, ageByGroupMax.value, 5);

  const patientsBySex = data
    .filter((row) => row.name === "sex")[0]
    ["statistics"].map((row) => [row.label, row.value])
    .sort((current, next) => (current[1] < next[1] ? 1 : -1));
  sexAtBirth.value = Object.fromEntries(patientsBySex);
}

async function loadData() {
  await getOrganisations();
  await getStatistics();
}

onMounted(() => {
  loadData()
    .catch((err) => {
      if (err.response) {
        error.value = err.response.errors[0].message;
      } else {
        error.value = err;
      }
    })
    .finally(() => (loading.value = false));
});
</script>
