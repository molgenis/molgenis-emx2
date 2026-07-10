<script setup lang="ts">
import { ref } from "vue";
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
  // @ts-ignore
} from "molgenis-viz";

import Breadcrumbs from "../components/breadcrumbs.vue";
import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent.js";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations.js";
import { asDataObject } from "../../../tailwind-components/app/utils/viz";
import type { IRecordStringNumber } from "../../../metadata-utils/src/viz/types.js";
import type {
  IComponents,
  IStatistics,
  IOrganisations,
} from "../../../metadata-utils/src/viz/ErnDashboard.js";

interface IWorkstreamTable {
  workstream: string;
  total: string;
  percent: string;
}

const loading = ref<boolean>(true);
const error = ref<Error | string>();
const patientsByWorksteamChart = ref<IComponents>();
const patientsByWorksteamData = ref<IWorkstreamTable[]>();
const sexAtBirthChart = ref<IComponents>();
const sexAtBirthData = ref<IRecordStringNumber>();
const providersData = ref<IOrganisations[]>();

async function getData() {
  const workstreamResponse = await getComponentStats(
    "../api/graphql",
    "patients-by-workstream"
  );

  const sexAtBirthResponse = await getComponentStats(
    "../api/graphql",
    "patients-sex-at-birth"
  );

  const providersResponse = await getOrganisations("../api/graphql");

  patientsByWorksteamChart.value = workstreamResponse[0];
  sexAtBirthChart.value = sexAtBirthResponse[0];
  providersData.value = providersResponse;
}

function prepareData() {
  sexAtBirthData.value = asDataObject(
    sexAtBirthChart.value?.statistics as IStatistics[],
    "label",
    "value",
    true
  );

  patientsByWorksteamData.value = patientsByWorksteamChart.value?.statistics
    ?.map((row: IStatistics) => {
      const percent = `${Math.round(
        parseFloat(row.value as unknown as string) * 100
      )}%`;
      return {
        workstream: row.label as string,
        total: row.description as string,
        percent: percent,
      };
    })
    .sort((current: IWorkstreamTable, next: IWorkstreamTable) => {
      return current.workstream.localeCompare(next.workstream);
    });

  providersData.value = providersData.value?.map((row: IOrganisations) => {
    let status: string = "No Data";
    if (row.providerInformation) {
      status = row.providerInformation[0].hasSubmittedData
        ? "Data Submitted"
        : "No Data";
    }
    return { ...row, hasSubmittedData: status };
  });
}

getData()
  .then(() => prepareData())
  .catch((err) => (error.value = err))
  .finally(() => (loading.value = false));
</script>

<template>
  <Page class="page-dashboard">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Dashboard"
      imageSrc="img/banner-diagnoses.jpg"
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
            :data="patientsByWorksteamData"
            :columnOrder="['workstream', 'total', 'percent']"
          />
        </DashboardChart>
        <DashboardChart id="sexAtBirthChart">
          <PieChart2
            chartId="cranio-sex-at-birth"
            title="Patients by sex at birth"
            :chartData="sexAtBirthData"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartHeight="225"
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
            :chartData="providersData"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            group="hasSubmittedData"
            :groupColorMappings="{
              'Data Submitted': '#f1681d',
              'No Data': '#f0f0f0',
            }"
            :tooltipTemplate="
              (row: IOrganisations) => {
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
