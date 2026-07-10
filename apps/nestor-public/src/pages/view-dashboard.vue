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

import type {
  IComponents,
  IOrganisations,
  IStatistics,
} from "../../../metadata-utils/src/viz/ErnDashboard";
import { getComponentStats } from "../../../metadata-utils/src/viz/getErnDashboardComponent";
import { getOrganisations } from "../../../metadata-utils/src/viz/getErnDashboardOrganisations";
import type { IRecordStringNumber } from "../../../metadata-utils/src/viz/types";
import * as NlGeoJson from "../../../molgenis-viz/src/data/nl.geo.json";
import {
  asDataObject,
  generateAxisTickData,
} from "../../../tailwind-components/app/utils/viz";
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
const organisationsData = ref<IOrganisations[]>([]);

const orgGroupMapping = {
  "Data Submitted": "#E9724C",
  "No Data": "#F0F0F0",
};

loadData().then(prepareData);

async function loadData() {
  try {
    const organisationsResponse = await getOrganisations("../api/graphql");
    const highlightsResponse = await getComponentStats(
      "../api/graphql",
      "data-highlights"
    );
    const sexAtBirthResponse = await getComponentStats(
      "../api/graphql",
      "pie-sex-at-birth"
    );
    const ageResponse = await getComponentStats(
      "../api/graphql",
      "barchart-age"
    );
    const enrollmentResponse = await getComponentStats(
      "../api/graphql",
      "table-enrollment-disease-group"
    );

    highlightsChart.value = highlightsResponse[0];
    sexAtBirthChart.value = sexAtBirthResponse[0];
    ageAtInclusionChart.value = ageResponse[0];
    enrollmentChart.value = enrollmentResponse[0];
    organisationsData.value = organisationsResponse;
    // await getStats();
  } catch (err) {
    error.value = err as Error;
  }
  loading.value = false;
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
</script>

<template>
  <Page id="page-dashboard">
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
            :groupColorMappings="orgGroupMapping"
            :legendData="orgGroupMapping"
            :chartSize="114"
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
            :chartHeight="225"
            :chartMargins="{ top: 10, right: 0, bottom: 60, left: 60 }"
            :columnPaddingInner="0.2"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<style>
.d3-viz {
  &.d3-pie,
  &.d3-geo-mercator {
    .chart-context {
      text-align: center;
      .chart-title {
        font-size: 1.1rem;
        padding: 0;
        margin-bottom: 0.5em;
        text-align: center;
      }
    }
  }

  &.d3-column-chart {
    .chart-title {
      font-size: 1.1rem;
      padding: 0;
      margin-bottom: 0.5em;
      text-align: center;
    }
  }
}

#sexAtBirthChart {
  .chart-area {
    .pie-labels {
      .pie-label-text {
        font-size: 0.7rem !important;
      }
    }
  }
}

#registryInstitutionsMap + .d3-viz-legend {
  padding: 0.6em 0.8em;
  label {
    margin-bottom: 0;
  }
}

#diseaseGroupEnrollment {
  caption {
    font-size: 1.1rem;
    padding: 0;
    margin-bottom: 0.5em;
    text-align: center;
  }
  thead {
    th {
      font-size: 0.8rem;
    }
  }
  tbody {
    td {
      font-size: 0.9rem;
      padding: 0.6em 0.2em;

      &[data-value="Undetermined"],
      &[data-value="Undetermined"] + td {
        background-color: var(--white);
        span {
          color: var(--gray-400);
        }
      }
    }
  }
}

#registryHighlights {
  .data-highlights {
    .data-highlight {
      padding: 0.8em 1em;
      .data-label {
        margin-bottom: 0.15em;
        font-size: 0.75rem;
      }

      .data-value {
        &::after {
          font-size: 1.8rem;
        }
      }
    }
  }
}

#nestorPublicDashboard {
  .dashboard-content {
    @media (min-width: 1800px) {
      max-width: 60vw;
    }
  }
}
</style>
