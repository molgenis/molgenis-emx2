<template>
  <Page class="page-dashboard">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Dashboard"
      height="medium"
    />
    <Breadcrumbs />
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
    <Dashboard id="publicDashboard" v-else>
      <DashboardBox class="dashboard-box viz-table">
        <DataTable
          tableId="workstreamSummary"
          caption="Percentage of patients by workstream"
          :data="workstreamSummary"
          :columnOrder="['workstream', 'percent']"
        />
      </DashboardBox>
      <DashboardBox class="viz-pie-chart">
        <PieChart
          chartId="sexAtBirth"
          :chartData="sexAtBirth"
          :chartHeight="265"
          :chartMargins="5"
          title="Sex at birth"
          :asDonutChart="true"
        />
      </DashboardBox>
      <DashboardBox class="viz-map">
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
            'No Data': '#f0f0f0',
          }"
          :tooltipTemplate="
            (row) => {
              return `
              <p class='title'>${row.name}</p>
              <p class='location'>${row.city}, ${row.country}</p>
            `;
            }
          "
          :enableLegendClicks="true"
          :legendData="{
            'Data Submitted': '#f1681d',
            'No Data': '#f0f0f0',
          }"
          :pointRadius="7"
          :mapColors="{
            water: '#43abcc',
            land: '#93dbab',
            border: '#1f171a',
          }"
          :mapCenter="{ latitude: 13, longitude: 50 }"
          :zoomLimits="[0.3, 10]"
        />
      </DashboardBox>
    </Dashboard>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  Page,
  PageHeader,
  PageSection,
  Dashboard,
  DashboardBox,
  MessageBox,
  GeoMercator,
  DataTable,
  PieChart,
} from "molgenis-viz";

import Breadcrumbs from "../components/breadcrumbs.vue";

import {
  fetchData,
  subsetData,
  renameKey,
  asDataObject,
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
`;

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
`;

function getDashboardData() {
  loading.value = true;
  Promise.all([
    fetchData("/api/graphql", providersQuery),
    fetchData("/api/graphql", statsQuery),
  ])
    .then((response) => {
      const organisations = response[0].data.Organisations;
      const stats = response[1].data.Components;

      providers.value = organisations.map((row) => {
        return {
          ...row,
          hasSubmittedData: row.providerInformation[0].hasSubmittedData
            ? "Data Submitted"
            : "No Data",
        };
      });

      workstreamSummary.value = subsetData(
        stats,
        "name",
        "patients-by-workstream"
      )[0].statistics.map((row) => {
        return { ...row, value: `${Math.round(parseFloat(row.value) * 100)}%` };
      });

      renameKey(workstreamSummary.value, "label", "workstream");
      renameKey(workstreamSummary.value, "value", "percent");

      const patientsSex = subsetData(stats, "name", "patients-sex-at-birth")[0]
        .statistics;
      sexAtBirth.value = asDataObject(patientsSex, "label", "value");
    })
    .then(() => {
      loading.value = false;
    })
    .catch((response) => {
      const err = JSON.parse(response.message);
      error.value = `${err.message} (${err.status})`;
    });
}

onMounted(() => getDashboardData());
</script>

<style lang="scss">
#publicDashboard {
  .dashboard-content {
    display: grid;
    grid-template-areas:
      "Table"
      "PieChart"
      "Map";
    gap: 2em;

    .dashboard-box {
      flex-grow: 1;
      box-sizing: content-box;

      &.viz-map {
        grid-area: Map;
        h3 {
          margin-top: 1em;
          margin-left: 1em;
        }
      }

      caption,
      h3 {
        text-align: center;
        font-size: 16pt;
        padding: 0;
      }

      &.viz-table {
        grid-area: Table;
      }

      &.viz-pie-chart {
        grid-area: PieChart;
      }

      &.viz-table,
      &.viz-pie-chart {
        padding: 1em;
      }
    }

    @media (min-width: 1182px) {
      grid-template-areas:
        "Table PieChart"
        "Map Map";
    }

    @media (min-width: 1524px) {
      max-width: 60vw;
    }
  }
}

#dataProvidersMap + .d3-viz-legend {
  top: auto;
  bottom: 0;
  box-shadow: 4px -2px 4px 2px hsla(0, 0%, 0%, 0.2);
}

#workstreamSummary {
  @media (min-width: 892px) {
    .column-header-percent,
    .column-percent {
      text-align: right;
    }
  }
}

</style>