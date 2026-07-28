<script setup lang="ts">
import { ref } from "vue";
import {
  ColumnChart,
  Dashboard,
  DashboardChart,
  DashboardRow,
  DataTable,
  DataValueHighlights,
  GeoMercator,
  LoadingScreen,
  MessageBox,
  Page,
  PieChart2,
  //@ts-expect-error
} from "molgenis-viz";

import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations";
import * as NlGeoJson from "../../../molgenis-viz/src/data/nl.geo.json";
import {
  asDataObject,
  generateAxisTickData,
} from "../../../tailwind-components/app/utils/viz";

import type { IRecordStringNumber } from "../../../metadata-utils/src/viz/types";
import type { NumericAxisTickData } from "../../../tailwind-components/types/viz";
import type {
  IComponents,
  IOrganisations,
  IStatistics,
} from "../../../metadata-utils/src/viz/ErnDashboard";

const loading = ref<boolean>(true);
const error = ref<Error | null>(null);
const highlightsChart = ref<IComponents>();
const highlightsData = ref<IRecordStringNumber>();
const sexAtBirthChart = ref<IComponents>();
const sexAtBirthData = ref<IRecordStringNumber>();
const ageAtInclusionChart = ref<IComponents>();
const ageAtInclusionData = ref<IStatistics[]>();
const ageAtInclusionAxis = ref<NumericAxisTickData>();
const enrollmentChart = ref<IComponents>();
const enrollmentData = ref<IStatistics[]>();
const organisationsData = ref<IOrganisations[]>([]);

loadData()
  .then(() => prepareData())
  .catch((err) => (error.value = err))
  .finally(() => (loading.value = false));

async function loadData() {
  const highlightsResponse = await getComponentStats(
    "../api/graphql",
    "dataHighlights"
  );
  const sexAtBirthResponse = await getComponentStats(
    "../api/graphql",
    "sexAtBirth"
  );
  const ageResponse = await getComponentStats(
    "../api/graphql",
    "ageAtLastFollowUp"
  );
  const enrollmentResponse = await getComponentStats(
    "../api/graphql",
    "enrollmentByDiseaseGroup"
  );
  const organisationsResponse = await getOrganisations("../api/graphql");

  highlightsChart.value = highlightsResponse[0];
  sexAtBirthChart.value = sexAtBirthResponse[0];
  ageAtInclusionChart.value = ageResponse[0];
  enrollmentChart.value = enrollmentResponse[0];
  organisationsData.value = organisationsResponse;
}

function prepareData() {
  highlightsData.value = asDataObject(
    highlightsChart.value?.statistics as IStatistics[],
    "label",
    "value"
  );

  sexAtBirthData.value = asDataObject(
    sexAtBirthChart.value?.statistics as IStatistics[],
    "label",
    "value",
    true
  );

  ageAtInclusionData.value = ageAtInclusionChart.value?.statistics;
  ageAtInclusionAxis.value = generateAxisTickData(
    ageAtInclusionData.value as IStatistics[],
    "value"
  );

  enrollmentData.value = (enrollmentChart.value?.statistics as IStatistics[])
    .map((row: IStatistics) => {
      return {
        ...row,
        "Thematic Disease Group": row.label,
        "Number of Patients": row.value,
      };
    })
    .sort((current: IStatistics, next: IStatistics) => {
      return (current.valueOrder as number) < (next.valueOrder as number)
        ? -1
        : 1;
    });

  organisationsData.value = organisationsData.value?.map(
    (row: IOrganisations) => {
      let submissionStatus: string = "No Data";
      if (
        row.providerInformation &&
        row.providerInformation?.[0]?.hasSubmittedData
      ) {
        submissionStatus = "Data Submitted";
      }
      return { ...row, hasSubmittedData: submissionStatus };
    }
  );
}
</script>

<template>
  <Page id="nestorPublicDashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      id="nestorPublicDashboard"
      :verticalPadding="0"
      :horizontalPadding="2"
      v-else
    >
      <DashboardRow id="registryHighlights" :columns="1">
        <DataValueHighlights
          title="NESTOR Registry at a glance"
          :data="highlightsData"
        />
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <GeoMercator
            chartId="registryInstitutionsMap"
            title="Status of data by healthcare provider"
            :chartData="organisationsData"
            rowId="code"
            longitude="longitude"
            latitude="latitude"
            :geojson="NlGeoJson"
            group="hasSubmittedData"
            :groupColorMappings="{
              'Data Submitted': '#f2846e',
              'No Data': '#F0F0F0',
            }"
            :legendData="{
              'Data Submitted': '#f2846e',
              'No Data': '#F0F0F0',
            }"
            :chartHeight="350"
            :mapCenter="{
              // centroid of the Netherlands
              latitude: 5.291266,
              longitude: 52.132633,
            }"
            :mapColors="{
              land: '#185f5b',
              border: '#08211F',
              water: '#D8DDE9',
            }"
            :pointRadius="6"
            :tooltipTemplate="
              (row: IOrganisations) => {
                return `
                <p class='title'>${row.name}</p>
                <p class='location'>${row.city}, ${row.country}</p>
            `;
              }
            "
            :chartScale="10"
            :zoomLimits="[0, 10]"
            :enableLegendClicks="true"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            tableId="diseaseGroupEnrollment"
            caption="Summary of patients enrolled by thematic disease group"
            :data="enrollmentData"
            :columnOrder="['Thematic Disease Group', 'Number of Patients']"
            :enableRowHighlighting="true"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <PieChart2
            chartId="sexAtBirthChart"
            title="Sex at birth"
            :chartData="sexAtBirthData"
            legendPosition="bottom"
            :chartHeight="150"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="10"
          />
        </DashboardChart>
        <DashboardChart>
          <ColumnChart
            chartId="registry-age-at-inclusion"
            title="Age at last follow-up"
            :chartData="ageAtInclusionData"
            xvar="label"
            yvar="value"
            :yMin="0"
            :yMax="ageAtInclusionAxis?.limit"
            :yTickValues="ageAtInclusionAxis?.ticks"
            xAxisLabel="Age groups"
            yAxisLabel="Number of patients"
            columnFill="#185f5b"
            columnHoverFill="#f2846e"
            :chartHeight="225"
            :chartMargins="{ top: 10, right: 0, bottom: 60, left: 60 }"
            :columnPaddingInner="0.2"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>
