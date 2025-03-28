<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Dashboard"
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
      <DashboardRow :columns="2">
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
            :geojson="geojson"
            :chartSize="114"
            :chartHeight="350"
            :mapCenter="{
              // centroid of the Netherlands
              latitude: 5.291266,
              longitude: 52.132633,
            }"
            :pointRadius="6"
            :tooltipTemplate="(row: IOrganisations) => {
              return `
                <p class='title'>${row.name}</p>
                <p class='location'>${row.city}, ${row.country}</p>
                `;
            }"
            :mapColors="{
              water: 'hsl(177, 63%, 90%)',
              land: 'hsl(177, 63%, 35%)',
              border: 'hsl(177, 63%, 90%)',
            }"
            :chartScale="10"
            :zoomLimits="[0, 10]"
            :enableLegendClicks="true"
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            :tableId="enrollmentChart?.chartId"
            :caption="enrollmentChart?.chartTitle"
            :columnOrder="['name', 'value']"
            :data="enrollmentChartData"
            :enableRowHighlighting="true"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <PieChart2
            :chartId="genderChart?.chartId"
            :title="genderChart?.chartTitle"
            :chartData="genderChartData"
            :chartColors="genderChartPalette"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :legendPosition="genderChart?.legendPosition?.name"
            :chartMargins="10"
            :chartHeight="215"
            :chartScale="0.85"
          />
        </DashboardChart>
        <DashboardChart>
          <ColumnChart
            :chartId="ageChart?.chartId"
            :title="ageChart?.chartTitle"
            :chartData="ageChartData"
            xvar="name"
            yvar="value"
            :xAxisLabel="ageChart?.xAxisLabel"
            :yAxisLabel="ageChart?.yAxisLabel"
            :yTickValues="ageChart?.yAxisTicks"
            columnFill="hsl(177,63%,37%)"
            columnHoverFill="hsl(177,63%,67%)"
            :yMin="ageChart?.yAxisMinValue"
            :yMax="ageChart?.yAxisMaxValue"
            :chartHeight="275"
            :chartMargins="{
              top: ageChart?.topMargin,
              right: ageChart?.rightMargin,
              bottom: ageChart?.bottomMargin,
              left: ageChart?.leftMargin,
            }"
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
  DataTable,
  PieChart2,
  ColumnChart,
  // @ts-ignore
} from "molgenis-viz";

import { asKeyValuePairs } from "../../../cranio-provider/src/utils/index";
import { generateAxisTickData } from "../utils/generateAxisTicks";
import type {
  ICharts,
  IChartData,
  IOrganisations,
  IOrganisationsResponse,
  IDashboardPagesResponse,
} from "../types/schema";
import type { IKeyValuePair } from "../types";

import * as geojson from "../data/nl.geo.json";

const loading = ref<boolean>(true);
const error = ref<Error | null>(null);

const OrganisationsChart = ref<ICharts>();
const OrganisationsChartData = ref<IOrganisations[]>([]);
const OrganisationsChartPalette = ref<IKeyValuePair>({});
const enrollmentChart = ref<ICharts>();
const enrollmentChartData = ref<IChartData[]>();
const ageChart = ref<ICharts>();
const ageChartData = ref<IChartData[]>();
const genderChart = ref<ICharts>();
const genderChartData = ref<IKeyValuePair>();
const genderChartPalette = ref<IKeyValuePair>({});

async function getPageCharts() {
  const response: IDashboardPagesResponse = await request(
    "../api/graphql",
    gql`
      {
        DashboardPages {
          name
          charts {
            chartId
            chartType {
              name
            }
            chartTitle
            chartSubtitle
            xAxisLabel
            xAxisMinValue
            xAxisMaxValue
            xAxisTicks
            yAxisLabel
            yAxisMinValue
            yAxisMaxValue
            yAxisTicks
            colorPalette {
              key
              color
            }
            topMargin
            rightMargin
            bottomMargin
            leftMargin
            legendPosition {
              name
            }
            dataPoints {
              id
              name
              value
              valueLabel
              primaryCategory
              secondaryCategory
              primaryCategoryLabel
              secondaryCategoryLabel
              sortOrder
            }
          }
        }
      }
    `
  );

  const charts = response.DashboardPages[0].charts as ICharts[];

  OrganisationsChart.value = charts.filter(
    (chart: ICharts) => chart.chartId === "organisation-map"
  )[0];
  OrganisationsChartPalette.value = asKeyValuePairs(
    OrganisationsChart.value.colorPalette,
    "key",
    "color"
  );

  enrollmentChart.value = charts.filter(
    (chart: ICharts) => chart.chartId === "enrollment-by-disease-group"
  )[0];
  enrollmentChartData.value = enrollmentChart.value.dataPoints?.sort(
    (current: IChartData, next: IChartData) => {
      return (current.sortOrder as number) - (next.sortOrder as number);
    }
  );

  genderChart.value = charts.filter(
    (chart: ICharts) => chart.chartId === "sex-at-birth"
  )[0];

  const genderData = genderChart.value.dataPoints?.sort(
    (current: IChartData, next: IChartData) => {
      return (next.value as number) - (current.value as number);
    }
  );

  genderChartData.value = asKeyValuePairs(genderData, "name", "value");
  genderChartPalette.value = asKeyValuePairs(
    genderChart.value.colorPalette,
    "key",
    "color"
  );

  ageChart.value = charts.filter(
    (chart: ICharts) => chart.chartId === "age-at-last-visit"
  )[0];

  ageChartData.value = ageChart.value.dataPoints?.sort(
    (current: IChartData, next: IChartData) => {
      return (current.sortOrder as number) - (next.sortOrder as number);
    }
  );

  if (ageChartData.value) {
    const ageChartTickData = generateAxisTickData(ageChartData.value, "value");
    ageChart.value.yAxisMaxValue = ageChartTickData.limit;
    ageChart.value.yAxisTicks = ageChartTickData.ticks;
  }
}

async function getOrganisations() {
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
  await getPageCharts();
  await getOrganisations();
}

onMounted(() => {
  loadData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
