<template>
  <Page class="page-dashboard">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Dashboard"
      imageSrc="banner-diagnoses.jpg"
    />
    <Breadcrumbs />
    <LoadingScreen v-if="loading && !error" />
    <PageSection
      id="section-intro-title"
      aria-labelledby="section-intro-title"
      :verticalPadding="2"
      v-else-if="!loading && error"
    >
      <MessageBox type="error">
        <p>Unable to retrieve data: {{ error }}</p>
      </MessageBox>
    </PageSection>
    <Dashboard id="cranioPublicDashboard" :horizontalPadding="5" v-else>
      <DashboardRow :columns="2">
        <DashboardChart>
          <DataTable
            tableId="workstreamSummary"
            caption="Percentage of patients by workstream"
            :data="workstreamSummary"
            :columnOrder="['workstream', 'percent']"
          />
        </DashboardChart>
        <DashboardChart id="sexAtBirthChart">
          <PieChart2
            chartId="cranio-sex-at-birth"
            title="Patients by sex at birth"
            :chartData="sexAtBirth"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartHeight="185"
            legendPosition="bottom"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="1">
        <DashboardChart
          :verticalPadding="0"
          :horizontalPadding="0"
          class="viz-map"
        >
          <GeoMercator
            chartId="data-providers-map"
            title="Data Providers"
            :geojson="WorldGeoJson"
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
                return `<p class='title'>${row.name}</p><p class='location'>${row.city}, ${row.country}</p>`;
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
        </DashboardChart>
      </DashboardRow>
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
  DashboardRow,
  DashboardChart,
  LoadingScreen,
  MessageBox,
  WorldGeoJson,
  GeoMercator,
  DataTable,
  PieChart2,
  asDataObject,
  renameKey,
} from "molgenis-viz";

import Breadcrumbs from "../components/breadcrumbs.vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

let loading = ref(true);
let error = ref(false);
let stats = ref([]);
let providers = ref([]);
let workstreamSummary = ref([]);
let sexAtBirth = ref({});

async function getStatsByComponent() {
  const query = gql`
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

  const response = await request("../api/graphql", query);
  const data = response.Components;
  stats.value = data;
}

async function getOrganisations() {
  const query = `{
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
  }`;

  const response = await request("../api/graphql", query);
  const data = response.Organisations.map((row) => {
    return {
      ...row,
      hasSubmittedData: row.providerInformation[0].hasSubmittedData
        ? "Data Submitted"
        : "No Data",
    };
  });
  providers.value = data;
}

async function loadData() {
  await getStatsByComponent();
  await getOrganisations();
}

onMounted(() => {
  loadData()
    .then(() => {
      const workstreamComponent = stats.value.filter(
        (row) => row.name === "patients-by-workstream"
      )[0];
      workstreamSummary.value = workstreamComponent.statistics
        .map((row) => {
          return {
            ...row,
            value: `${Math.round(parseFloat(row.value) * 100)}%`,
          };
        })
        .sort((current, next) => (current.label < next.label ? -1 : 1));
      renameKey(workstreamSummary.value, "label", "workstream");
      renameKey(workstreamSummary.value, "value", "percent");

      const sexAtBirthStats = stats.value
        .filter((row) => row.name === "patients-sex-at-birth")[0]
        .statistics.sort((current, next) =>
          current.valueOrder < next.valueOrder ? -1 : 1
        );
      sexAtBirth.value = asDataObject(sexAtBirthStats, "label", "value");
    })
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
});
</script>

<style lang="scss">
#cranioPublicDashboard {
  .dashboard-content {
    .dashboard-box {
      &.viz-map {
        h3 {
          margin-top: 1em;
          margin-left: 1em;
        }
      }

      caption,
      h3 {
        @include setChartTitle($font-size: 1.15rem);
      }

      &.viz-table,
      &.viz-pie-chart {
        padding: 1em;
      }
    }

    @media (min-width: 1524px) {
      max-width: 60vw;
    }
  }
}

#workstreamSummary {
  th,
  td {
    padding: 0.6em 0.3em;
    font-size: 1rem;

    &[data-column-name="percent"] {
      text-align: right;
    }
  }
}

#sexAtBirthChart {
  .d3-pie {
    .chart-legend {
      .legend-item {
        .text-item {
          .item-label {
            font-size: 1rem;
          }
        }
      }
    }

    .chart {
      .chart-area {
        .pie-labels {
          .pie-label-text {
            font-size: 0.8rem !important;
          }
        }
      }
    }
  }
}

#data-providers-map + .d3-viz-legend {
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
