<template>
  <Page id="page-dashboard">
    <LoadingScreen v-if="loading" />
    <div class="page-section padding-h-2" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
    </div>
    <Dashboard :verticalPadding="0" :horizontalPadding="2" v-else>
      <DashboardRow id="registryHighlights" :columns="1">
        <DataValueHighlights 
          title="ern genturis registry at a glance"
          :data="registryHighlights"
        />
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <GeoMercator
            chartId="registryInstitutionsMap"
            title="Status of data by healthcare provider"
            :chartData="organisations"
            rowId="code"
            longitude="longitude"
            latitude="latitude"
            :geojson="WorldGeoJson"
            groupingVariable="hasSubmittedData"
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
              (row) => {
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
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <PieChart2
            chartId="sexAtBirthChart"
            title="Sex at birth"
            :chartData="sexAtBirth"
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
            :chartData="ageAtInclusion"
            xvar="label"
            yvar="value"
            :yTickValues="ageAtInclusionTicks"
            xAxisLabel="Age groups"
            yAxisLabel="Number of patients"
            :chartHeight="225"
            :chartMargins="{top: 10, right: 0, bottom: 60, left: 60}"
            :columnPaddingInner="0.2"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup>
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
  WorldGeoJson,
  PieChart2,
  ColumnChart,
  DataTable,
  DataValueHighlights,
} from "molgenis-viz";

import { seqAlongBy } from "../utils/utils";
import { max } from "d3";
const d3 = { max };

let loading = ref(true);
let error = ref(null);
let registryHighlights = ref({});
let sexAtBirth = ref({});
let ageAtInclusion = ref([]);
let ageAtInclusionTicks = ref([]);
let enrollmentData = ref({});
let organisations = ref([]);

const orgGroupMapping = {
  "Data Submitted": "#E9724C",
  "No Data": "#F0F0F0",
};

async function getOrganisations() {
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
  const response = await request("../api/graphql", query);
  const data = response.Organisations.map((row) => {
    const status = row.providerInformation[0].hasSubmittedData
      ? "Data Submitted"
      : "No Data";
    return { ...row, hasSubmittedData: status };
  });
  organisations.value = data;
}

async function getStats() {
  const query = gql`
    {
      Components {
        name
        statistics {
          id
          label
          value
          valueOrder
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = response.Components;

  const highlights = data.filter((row) => row.name === "data-highlights");
  registryHighlights.value = Object.fromEntries(
    highlights[0]["statistics"].map((row) => [row.label, row.value])
  );

  const sexData = data.filter((row) => row.name === "pie-sex-at-birth");
  sexAtBirth.value = Object.fromEntries(
    sexData[0]["statistics"]
      .map((row) => [row.label, row.value])
      .sort((current, next) => (current[1] < next[1] ? 1 : -1))
  );

  const age = data.filter((row) => row.name === "barchart-age");
  ageAtInclusion.value = age[0]["statistics"];
  
  const maxValue = d3.max(ageAtInclusion.value, row => row.value)
  const ymax = Math.round( maxValue / 10) * 10;
  ageAtInclusionTicks.value = seqAlongBy(0, ymax, 25);

  enrollmentData.value = data
    .filter((row) => row.name === "table-enrollment-disease-group")[0]["statistics"]
    .filter(row => row.label !== 'Undetermined' && row.value !== 0)
    .sort((current,next) => current.valueOrder < next.valueOrder ? -1 : 1)
    .map((row) => {
      return {
        ...row,
        "Thematic Disease Group": row.label,
        "Number of Patients": row.value,
      };
    });
}

async function loadData() {
  await getOrganisations();
  await getStats();
}

onMounted(() => {
  loadData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">

@mixin chartTitle {
  font-size: 1.1rem;
  padding: 0;
  margin-bottom: 0.5em;
  text-align: center;
}

.d3-viz {
  &.d3-pie, &.d3-geo-mercator {
    .chart-context {
      text-align: center;
      .chart-title {
        @include chartTitle;
      }
    }
  }
  
  
  &.d3-column-chart {
    .chart-title {
      @include chartTitle;
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
    @include chartTitle;
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
        background-color: $gray-000;
        span {
          color: $gray-400;
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
</style>