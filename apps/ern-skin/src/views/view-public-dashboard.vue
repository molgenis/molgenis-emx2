<template>
  <Page id="page-dashboard">
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
              (row: Organisations) => {
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
            columnFill="#02818a"
            xvar="category"
            yvar="value"
            xAxisLineBreaker=";"
            :yMin="0"
            :yMax="ageByGroupYAxis.limit"
            :yTickValues="ageByGroupYAxis.ticks"
            :chartHeight="225"
            :chartMargins="{ top: 25, right: 5, bottom: 40, left: 50 }"
            :columnPaddingInner="0.2"
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

<script setup lang="ts">
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
  // @ts-ignore
} from "molgenis-viz";

import type { IStatistics } from "../types/ernskin";
import type {
  IKeyValuePair,
  sexAtBirthData,
  OrganisationsResponse,
  Organisations,
  ComponentsResponse,
  Components,
  DashboardHighlights,
  PatientsByGroup,
  IAgeByGroup,
} from "../types/dashboard";

import { generateAxisTickData } from "../../../tailwind-components/app/utils/viz";
import type { NumericAxisTickData } from "../../../tailwind-components/types/viz";

const loading = ref<boolean>(true);
const error = ref<string>();
const registryHighlights = ref<DashboardHighlights>({
  Patients: 0,
  "Member countries": 0,
  "Healthcare providers": 0,
});
const organisations = ref<Organisations[]>([]);
const ageByGroup = ref<IAgeByGroup[]>([]);
const patientsByGroup = ref<PatientsByGroup>();
const sexAtBirth = ref<sexAtBirthData>({
  Female: 0,
  Intersex: 0,
  Male: 0,
  Undetermined: 0,
});

const ageByGroupYAxis = ref<NumericAxisTickData>({
  limit: 0,
  ticks: [] as number[],
  min: 0,
  max: 0,
});

async function getOrganisations(): Promise<Organisations[]> {
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
  const response: OrganisationsResponse = await request(
    "../api/graphql",
    query
  );
  return response.Organisations;
}

async function getComponents(): Promise<Components[]> {
  const query = gql`
    {
      Components {
        name
        statistics {
          label
          value
          valueOrder
          description
        }
      }
    }
  `;
  const response: ComponentsResponse = await request("../api/graphql", query);
  return response.Components as Components[];
}

function prepDataHighlights(data: Components) {
  const highlightValues = data.statistics.map((row: IStatistics) => [
    row.label,
    row.value,
  ]);
  registryHighlights.value = Object.fromEntries(highlightValues);
}

function prepEnrolment(data: Components) {
  const enrolmentData = data.statistics.map((row) => {
    return {
      ...row,
      "thematic disease group": row.label,
      patients: row.value,
    };
  });
  patientsByGroup.value = enrolmentData as unknown as PatientsByGroup;
}

function prepAgeByGroup(data: Components) {
  const ageByGroupData = data.statistics
    .sort((current: IStatistics, next: IStatistics) =>
      (current.valueOrder as number) < (next.valueOrder as number) ? -1 : 1
    )
    .map((row: IStatistics) => {
      return {
        ...row,
        category: `${row.label};${row.description}`,
      };
    });
  ageByGroup.value = ageByGroupData;
}

function prepGender(data: Components) {
  const genderData = data.statistics
    .filter((row: IStatistics) => row.value && row.value > 0)
    .map((row: IStatistics) => [row.label, row.value])
    .sort((current, next) => {
      return (current[1] as number) < (next[1] as number) ? 1 : -1;
    });
  sexAtBirth.value = Object.fromEntries(genderData);
}

onMounted(async () => {
  try {
    const orgData = await getOrganisations();
    const components = await getComponents();

    organisations.value = orgData.map((row: Organisations) => {
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
      };
    });

    const ageChart = components.filter(
      (row: Components) => row.name === "age"
    )[0];
    const highlights = components.filter(
      (row: Components) => row.name === "highlights"
    )[0];
    const enrolment = components.filter(
      (row: Components) => row.name === "enrolment"
    )[0];
    const gender = components.filter(
      (row: Components) => row.name === "sex"
    )[0];

    prepDataHighlights(highlights);
    prepEnrolment(enrolment);
    prepAgeByGroup(ageChart);
    prepGender(gender);

    if (ageByGroup.value) {
      ageByGroupYAxis.value = generateAxisTickData(ageByGroup.value, "value");
    }

    loading.value = false;
  } catch (err: any) {
    if (err.response) {
      error.value = err.response.errors[0].message;
    } else {
      error.value = err as string;
    }
  }
});
</script>

<style lang="scss">
@use "molgenis-viz/styles" as *;
@use "@/styles" as *;
@use "@/styles/variables" as *;

.d3-viz {
  &.d3-pie,
  &.d3-geo-mercator {
    .chart-context {
      text-align: center;
      .chart-title {
        @include setChartTitle;
      }
    }
  }

  &.d3-column-chart {
    .chart-title {
      @include setChartTitle;
    }
  }
}

#ernSkinOrganisationsMap {
  & + .d3-viz-legend {
    padding: 0.6em 0.8em;
    label {
      margin-bottom: 0;
    }
  }
}

#registryPatientsByGroup {
  caption {
    @include setChartTitle;
  }
  thead {
    th {
      font-size: 0.8rem;
    }
  }
  tbody {
    td {
      font-size: 0.88rem;
      padding: 0.5em 0.4em;
    }
  }
}

#skinRegistryHighlights {
  .data-highlight {
    padding: 0.8em 1em;
    .data-label {
      margin-bottom: 0.15em;
      font-size: 0.75rem;
      color: $gray-000;
    }

    .data-value {
      &::after {
        font-size: 1.8rem;
      }
    }
  }
}

#registryPatientsByAgeGroup {
  .chart-area {
    .chart-axes {
      .tick {
        text {
          tspan {
            font-size: 0.8em;
          }
        }
      }
    }
  }
}

#registryPatientsBySexAtBirth {
  .chart-area {
    .pie-labels {
      .pie-label-text {
        font-size: 0.7rem !important;
      }
    }
  }
}

.d3-pie > .chart-legend {
  .legend-item {
    .item-label {
      font-size: 0.95rem;
    }
  }
}
</style>
