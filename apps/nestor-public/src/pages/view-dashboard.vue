<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Dashboard"
      imageSrc="bkg-image-dashboard.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      v-else-if="!loading && !error"
      id="public-dashboard"
      :verticalPadding="0"
      :horizontalPadding="2"
    >
      <DashboardRow :columns="1">
        <DashboardChart>
          <GeoMercator
            :chartId="OrganisationsChart?.chartId"
            :title="OrganisationsChart?.chartTitle"
            :chartData="OrganisationsChartData"
            rowId="code"
            longitude="longitude"
            latitude="latitude"
            group="hasSubmittedData"
            :legendData="OrganisationsChartPalette"
            :groupColorMappings="OrganisationsChartPalette"
            :geojson="WorldGeoJson"
            :chartSize="114"
            :chartHeight="350"
            :mapCenter="{
              latitude: 5,
              longitude: 51,
            }"
            :pointRadius="6"
            :tooltipTemplate="(row: IOrganisations) => {
              return `
                <p class='title'>${row.name}</p>
                <p class='location'>${row.city}, ${row.country}</p>
                `;
            }"
            :chartScale="1.8"
            :zoomLimits="[0.5, 10]"
            :enableLegendClicks="true"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import {
  Page,
  PageHeader,
  Dashboard,
  DashboardRow,
  DashboardChart,
  LoadingScreen,
  MessageBox,
  GeoMercator,
  WorldGeoJson,
  // @ts-ignore
} from "molgenis-viz";

import { asKeyValuePairs } from "../../../cranio-provider/src/utils/index";
import type {
  ICharts,
  IOrganisations,
  IOrganisationsResponse,
  IChartsResponse,
} from "../types/schema";
import type { IKeyValuePair } from "../types";

const loading = ref<boolean>(true);
const error = ref<Error | null>(null);

const OrganisationsChart = ref<ICharts>();
const OrganisationsChartData = ref<IOrganisations[]>([]);
const OrganisationsChartPalette = ref<IKeyValuePair>({});

async function getOrganisations() {
  const chartSettings: IChartsResponse = await request(
    "../api/graphql",
    gql`
      {
        Charts(filter: { chartId: { equals: "organisation-map" } }) {
          chartId
          chartTitle
          colorPalette {
            key
            color
          }
        }
      }
    `
  );
  OrganisationsChart.value = chartSettings.Charts[0];
  OrganisationsChartPalette.value = asKeyValuePairs(
    OrganisationsChart.value.colorPalette,
    "key",
    "color"
  );

  const response: IOrganisationsResponse = await request(
    "../api/graphql",
    gql`
      {
        Organisations {
          name
          label
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
    `
  );
  const data: IOrganisations[] = response.Organisations.map(
    (row: IOrganisations) => {
      const status = row.providerInformation[0].hasSubmittedData
        ? "Data Submitted"
        : "No Data";
      return { ...row, hasSubmittedData: status };
    }
  );
  OrganisationsChartData.value = data;
}

async function loadData() {
  await getOrganisations();
}

onMounted(() => {
  loadData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
