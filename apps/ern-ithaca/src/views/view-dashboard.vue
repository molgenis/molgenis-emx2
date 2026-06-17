<script lang="ts" setup>
import { ref } from "vue";
import {
  Page,
  MessageBox,
  Dashboard,
  DashboardRow,
  DashboardChart,
  DataValueHighlights,
  DataTable,
  GeoMercator,
  WorldGeoJson,
  // @ts-expect-error
} from "molgenis-viz";

import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations";

import type {
  IComponents,
  IStatistics,
  IOrganisations,
} from "../../../metadata-utils/src/viz/ErnDashboard";
import type {
  IDashboardHighlights,
  ITopGenes,
  ITopDiagnoses,
  IEnrollment,
} from "../types";

const loading = ref<boolean>(true);
const error = ref<string>();

const dataHighlightsChart = ref<IComponents>();
const dataHighlightsData = ref<IDashboardHighlights>();
const topGenesChart = ref<IComponents>();
const topGenesData = ref<ITopGenes[]>();
const topDiagnosesChart = ref<IComponents>();
const topDiagnosesData = ref<ITopDiagnoses[]>();
const enrollmentChart = ref<IComponents>();
const enrollmentData = ref<IEnrollment[]>();
const organisationsMapData = ref<IOrganisations[]>();

const mapColorPalette = {
  "Full member": "#F1FAEE",
  "Affiliated partner": "#FFA69E",
  Hub: "#843B62",
};

async function loadData() {
  const highlightsResponse = await getComponentStats(
    "../api/graphql",
    "data highlights"
  );
  dataHighlightsChart.value = highlightsResponse[0];

  const topGenesResponse = await getComponentStats(
    "../api/graphql",
    "number-of-cases-by-gene"
  );
  topGenesChart.value = topGenesResponse[0];

  const topDiagnosesResponse = await getComponentStats(
    "../api/graphql",
    "number-of-cases-by-diagnosis"
  );
  topDiagnosesChart.value = topDiagnosesResponse[0];

  const enrollmentResponse = await getComponentStats(
    "../api/graphql",
    "participant-enrolment-by-subregistry"
  );
  enrollmentChart.value = enrollmentResponse[0];

  const organisationsResponse = await getOrganisations("../api/graphql");
  organisationsMapData.value = organisationsResponse;
}

loadData()
  .then(() => {
    // prepare data for data highlight component
    if (dataHighlightsChart.value && dataHighlightsChart.value.statistics) {
      const dataHighlightValues = dataHighlightsChart.value.statistics.map(
        (row: IStatistics) => {
          return [row.label, row.value];
        }
      );
      dataHighlightsData.value = Object.fromEntries(dataHighlightValues);
    }

    // prepare data for Number of Cases by Gene (Top 10)
    if (topGenesChart.value && topGenesChart.value.statistics) {
      topGenesData.value = topGenesChart.value.statistics.map(
        (row: IStatistics) => {
          return { gene: row.label, count: row.value } as ITopGenes;
        }
      );
    }

    // prepare data for: Number of cases by diagnoses (Top 10)
    if (topDiagnosesChart.value && topDiagnosesChart.value.statistics) {
      topDiagnosesData.value = topDiagnosesChart.value.statistics.map(
        (row: IStatistics) => {
          return { diagnosis: row.label, count: row.value } as ITopDiagnoses;
        }
      );
    }

    // prepare data for: Enrollment by subregistry
    if (enrollmentChart.value && enrollmentChart.value.statistics) {
      enrollmentData.value = enrollmentChart.value.statistics.map(
        (row: IStatistics) => {
          return { subregistry: row.label, count: row.value } as IEnrollment;
        }
      );
    }
  })
  .catch((err) => (error.value = err))
  .finally(() => (loading.value = false));
</script>

<template>
  <Page>
    <div class="page-section padding-h-2" v-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data. {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      id="publicDashboard"
      class="bg-blue-100"
      :verticalPadding="0"
      :horizontalPadding="3"
      v-else-if="!loading && !error"
    >
      <DashboardRow :columns="1">
        <DataValueHighlights
          id="ernIthacaDataHighlights"
          title="ERN ITHACA at a glance"
          :data="dataHighlightsData"
        />
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <DataTable
            :tableId="topGenesChart?.name"
            :caption="topGenesChart?.label"
            :data="topGenesData"
            :columnOrder="['gene', 'count']"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            :tableId="topDiagnosesChart?.name"
            :caption="topDiagnosesChart?.label"
            :data="topDiagnosesData"
            :columnOrder="['diagnosis', 'count']"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            :tableId="enrollmentChart?.name"
            :caption="enrollmentChart?.label"
            :data="enrollmentData"
            :columnOrder="['subregistry', 'count']"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="1">
        <DashboardChart
          id="provider-map"
          :verticalPadding="0"
          :horizontalPadding="0"
        >
          <GeoMercator
            chartId="ern-ithaca-organisations-map"
            :geojson="WorldGeoJson"
            :chartData="organisationsMapData"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            group="organisationType"
            :groupColorMappings="mapColorPalette"
            :legendData="mapColorPalette"
            :mapCenter="{
              latitude: 3,
              longitude: 51,
            }"
            :mapColors="{
              land: '#709190',
              border: '#061428',
              water: '#061428',
            }"
            :tooltipTemplate="
              (row: IOrganisations) => {
                return `
                <p class='title'>${row.name}</p>
                <p class='center-location'>
                  <span class='location-city'>${row.city}</span>
                  <span class='location-country'>${row.country}</span>
                </p>
                <p class='center-type'>${row.organisationType}</p>
                `;
              }
            "
            :zoomLimits="[0.3, 10]"
            :enableLegendClicks="true"
            :chartHeight="440"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

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
