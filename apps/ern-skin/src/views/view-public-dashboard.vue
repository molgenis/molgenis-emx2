<template>
  <Page id="skinPublicDashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data. {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      class="bg-blue-100"
      :verticalPadding="0"
      :horizontalPadding="2"
      v-else
    >
      <DashboardRow :columns="1">
        <DataValueHighlights
          id="skinRegistryHighlights"
          title="ERN Skin at a glance"
          :data="highlightsData"
        />
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <GeoMercator
            chartId="ernSkinOrganisationsMap"
            title="Status of data by healthcare provider"
            :geojson="WorldGeoJson"
            :chartData="organisationsData"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            group="hasSubmittedData"
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
              (row: IDataProviders) => {
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
            :chartHeight="440"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            tableId="registryPatientsByGroup"
            caption=" Summary of patients enrolled by thematic disease group"
            :data="patientsByGroupData"
            :columnOrder="['thematic disease group', 'patients']"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <ColumnChart
            chartId="registryPatientsByAgeGroup"
            title="Number of patients by age category"
            :chartData="ageGroupData"
            columnFill="#02818a"
            xvar="category"
            yvar="value"
            xAxisLineBreaker=";"
            :yMin="0"
            :yMax="ageGroupAxis?.limit"
            :yTickValues="ageGroupAxis?.ticks"
            :chartHeight="225"
            :chartMargins="{ top: 25, right: 2, bottom: 40, left: 25 }"
            :columnPaddingInner="0.2"
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="registryPatientsBySexAtBirth"
            title="Sex at birth"
            :chartData="sexAtBirthData"
            legendPosition="bottom"
            :chartHeight="165"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="5"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref } from "vue";
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
  // @ts-ignore
} from "molgenis-viz";

import type {
  IComponents,
  IStatistics,
  IOrganisations,
} from "../../../metadata-utils/src/viz/ErnDashboard";

import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations";
import {
  generateAxisTickData,
  asDataObject,
} from "../../../tailwind-components/app/utils/viz";

import type { IRecordStringNumber } from "../../../metadata-utils/src/viz/types";
import type { NumericAxisTickData } from "../../../tailwind-components/types/viz";

interface IDataProviders extends IOrganisations {
  providerIdentifier: string;
  hasSubmittedData: string;
}

const loading = ref<boolean>(true);
const error = ref<string>();
const highlightsChart = ref<IComponents>();
const highlightsData = ref<IRecordStringNumber>();
const ageGroupChart = ref<IComponents>();
const ageGroupData = ref<IStatistics[]>();
const ageGroupAxis = ref<NumericAxisTickData>();
const patientsByGroupChart = ref<IComponents>();
const patientsByGroupData = ref<IStatistics[]>();
const sexAtBirthChart = ref<IComponents>();
const sexAtBirthData = ref<IRecordStringNumber>();
const organisationsChart = ref<IOrganisations[]>();
const organisationsData = ref<IDataProviders[]>();

async function loadData() {
  const highlightsResponse = await getComponentStats(
    "../api/graphql",
    "highlights"
  );
  const patientsByGroupResponse = await getComponentStats(
    "../api/graphql",
    "enrolment"
  );
  const ageResponse = await getComponentStats("../api/graphql", "age");
  const sexResponse = await getComponentStats("../api/graphql", "sex");
  const organisationsResponse = await getOrganisations("../api/graphql");

  highlightsChart.value = highlightsResponse[0];
  patientsByGroupChart.value = patientsByGroupResponse[0];
  ageGroupChart.value = ageResponse[0];
  sexAtBirthChart.value = sexResponse[0];
  organisationsChart.value = organisationsResponse;
}

function prepareData() {
  highlightsData.value = asDataObject(
    highlightsChart.value?.statistics as IStatistics[],
    "label",
    "value"
  );

  patientsByGroupData.value = patientsByGroupChart.value?.statistics?.map(
    (row: IStatistics) => {
      return {
        ...row,
        "thematic disease group": row.label,
        patients: row.value,
      };
    }
  );

  ageGroupData.value = ageGroupChart.value?.statistics?.map(
    (row: IStatistics) => {
      return { ...row, category: `${row.label};${row.description}` };
    }
  );
  ageGroupAxis.value = generateAxisTickData(
    ageGroupData.value as IStatistics[],
    "value"
  );

  const sexAtBirthFiltered = sexAtBirthChart.value?.statistics?.filter(
    (row: IStatistics) => row.value && row.value > 0
  );
  sexAtBirthData.value = asDataObject(
    sexAtBirthFiltered as IStatistics[],
    "label",
    "value",
    true
  );

  organisationsData.value = organisationsChart.value?.map(
    (row: IOrganisations) => {
      const providerInformation =
        row.providerInformation && row.providerInformation[0]
          ? row.providerInformation[0]
          : undefined;
      return {
        ...row,
        hasSubmittedData: providerInformation?.hasSubmittedData
          ? "Submitted"
          : "Not Submitted",
        providerIdentifier: providerInformation?.providerIdentifier,
      } as IDataProviders;
    }
  );
}

loadData()
  .then(() => prepareData())
  .catch((err) => (error.value = err))
  .finally(() => (loading.value = false));
</script>
