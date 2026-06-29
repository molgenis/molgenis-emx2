<template>
  <Page id="page-dashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard
      id="genturisPublicDashboard"
      :verticalPadding="0"
      :horizontalPadding="2"
      v-else
    >
      <DashboardRow id="registryHighlights" :columns="1">
        <DataValueHighlights
          title="ern genturis registry at a glance"
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
            :geojson="WorldGeoJson"
            group="hasSubmittedData"
            :groupColorMappings="orgGroupMapping"
            :legendData="orgGroupMapping"
            :chartSize="114"
            :chartHeight="190"
            :mapCenter="{
              latitude: 5,
              longitude: 51,
            }"
            :pointRadius="4"
            :tooltipTemplate="
              (row: IOrganisations) => {
                return `
                <p class='title'>${row.name}</p>
                <p class='location'>${row.city}, ${row.country}</p>
            `;
              }
            "
            :zoomLimits="[0.3, 10]"
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
        <DashboardChart>
          <PieChart2
            chartId="sexAtBirthChart"
            title="Sex at birth"
            :chartData="sexAtBirthData"
            legendPosition="bottom"
            :chartHeight="140"
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
            :chartHeight="200"
            :chartMargins="{ top: 20, right: 10, bottom: 60, left: 60 }"
            :columnPaddingInner="0.2"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref } from "vue";

// @ts-ignore
import {
  Page,
  Dashboard,
  DashboardRow,
  DashboardChart,
  LoadingScreen,
  MessageBox,
  GeoMercator,
  WorldGeoJson,
  PieChart2,
  ColumnChart,
  DataTable,
  DataValueHighlights,
  // @ts-ignore
} from "molgenis-viz";

import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations";
import {
  generateAxisTickData,
  asDataObject,
} from "../../../tailwind-components/app/utils/viz";

import type {
  IComponents,
  IStatistics,
  IOrganisations,
} from "../../../metadata-utils/src/viz/ErnDashboard";
import type { IRecordStringNumber } from "../../../metadata-utils/src/viz/types";
import type { NumericAxisTickData } from "../../../tailwind-components/types/viz";

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
const organisationsData = ref<IOrganisations[]>();

const orgGroupMapping = {
  "Data Submitted": "#E9724C",
  "No Data": "#F0F0F0",
};

async function loadData() {
  const highlightsResponse = await getComponentStats(
    "../api/graphql",
    "data-highlights"
  );
  const sexAtBirthResponse = await getComponentStats(
    "../api/graphql",
    "pie-sex-at-birth"
  );
  const ageResponse = await getComponentStats("../api/graphql", "barchart-age");
  const enrollmentResponse = await getComponentStats(
    "../api/graphql",
    "table-enrollment-disease-group"
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
        row.providerInformation[0].hasSubmittedData
      ) {
        submissionStatus = "Data Submitted";
      }
      return { ...row, hasSubmittedData: submissionStatus };
    }
  );
}

loadData()
  .then(() => prepareData())
  .catch((err) => (error.value = err))
  .finally(() => (loading.value = false));
</script>
