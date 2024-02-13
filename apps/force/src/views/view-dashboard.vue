<template>
  <Page>
    <PageHeader title="FORCE Project" subtitle="Search and explore the data" />
    <div class="page-section" v-if="error">
      <MessageBox type="error">
        {{ error }}
      </MessageBox>
    </div>
    <div class="filters-bar">
      {{ filters }}
    </div>
    <Dashboard v-if="!loading && !error">
      <DashboardRow :columns="3">
        <DashboardChart>
          <BarChart
            chartId="research-centers-sum"
            title="Sum of cases by research center"
            :chartData="researchCenters"
            xvar="sum"
            yvar="researchCenter"
            :xMin="0"
            :xMax="6000"
            :xTickValues="[0, 1000, 2000, 3000, 4000, 5000, 6000]"
            xAxisLabel="Sum of cases"
            :barFill="palette[5]"
            :barHoverFill="palette[3]"
            :barPaddingOuter="0"
            :chartHeight="375"
            :chartMargins="{
              top: 5,
              right: 35,
              bottom: 60,
              left: 125,
            }"
            :enableClicks="true"
            @bar-clicked="
              (data) => onChartClick(JSON.parse(data), 'researchCenter')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            tableId="primary-tumors-sum"
            caption="Sum of primary tumors"
            :data="primaryTumors"
            :columnOrder="['primaryTumor', 'sum']"
            :enableRowClicks="true"
            @row-clicked="(data) => onChartClick(data, 'primaryTumor')"
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="sample-type-sum"
            title="Sum of sample types"
            :chartData="sampleTypes"
            :chartColors="sampleTypeColors"
            :chartHeight="225"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="10"
            legendPosition="bottom"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data) => onPieChartClick(data, 'sampleType')"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <ColumnChart
            chartId="sampling-period-sum"
            :chartData="samplingPeriods"
            xvar="samplingPeriod"
            yvar="sum"
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :chartHeight="210"
            :yTickValues="[0, 1000, 2000, 3000, 4000]"
            :yMax="4000"
            :enableClicks="true"
            @column-clicked="
              (data) => onChartClick(JSON.parse(data), 'samplingPeriod')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="sex-sum"
            title="Sum of sex"
            :chartData="sexCases"
            :chartColors="{
              Male: palette[2],
              Female: palette[5],
            }"
            :chartHeight="125"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="0"
            legendPosition="bottom"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data) => onPieChartClick(data, 'sex')"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup>
import gql from "graphql-tag";
import { request } from "graphql-request";
import { ref, onMounted } from "vue";

import {
  Page,
  Dashboard,
  DashboardChart,
  DashboardRow,
  PageHeader,
  BarChart,
  ColumnChart,
  DataTable,
  PieChart2,
  MessageBox,
} from "molgenis-viz";

import { schemePuBu as scheme } from "d3-scale-chromatic";
import { createPalette } from "../utils/index";

const palette = ref(scheme[6]);

let loading = ref(true);
let error = ref(false);
let researchCenters = ref([]);
let primaryTumors = ref([]);
let sampleTypes = ref({});
let samplingPeriods = ref([]);
let sexCases = ref({});
let sampleTypeColors = ref({});
let filters = ref({
  researchCenter: [],
  primaryTumor: [],
  sampleType: [],
  samplingPeriod: [],
  sex: [],
});

async function get(attribute) {
  const query = gql`{
    ClinicalData_groupBy {
      ${attribute} {
        name
      }
      _sum {
        n
      }
    }
  }`;
  const response = await request("../api/graphql", query);
  return response.ClinicalData_groupBy;
}

function onChartClick(data, attribute) {
  const value = data[attribute];
  if (filters.value[attribute].indexOf(value) === -1) {
    filters.value[attribute].push(value);
  }
}

function onPieChartClick(data, attribute) {
  const value = Object.keys(data)[0];
  if (filters.value[attribute].indexOf(value) === -1) {
    filters.value[attribute].push(value);
  }
}

async function getResearchCenters() {
  const data = await get("researchCenter");
  researchCenters.value = data
    .map(
      (row) =>
        new Object({ researchCenter: row.researchCenter.name, sum: row._sum.n })
    )
    .sort((current, next) => {
      return current.sum > next.sum ? -1 : 1;
    });
}

async function getPrimaryTumors() {
  const data = await get("primaryTumor");
  primaryTumors.value = data
    .map(
      (row) =>
        new Object({ primaryTumor: row.primaryTumor.name, sum: row._sum.n })
    )
    .sort((current, next) => {
      return current.primaryTumor > next.primaryTumor ? 1 : -1;
    });
}

async function getSampleType() {
  const data = await get("sampleType");
  const total = data.reduce((sum, curr) => curr._sum.n + sum, 0);
  const summarized = data
    .map(
      (row) =>
        new Object({
          sampleType: row.sampleType.name,
          sum: row._sum.n,
          percent: row._sum.n / total,
        })
    )
    .sort((current, next) => (current.sum > next.sum ? -1 : 1))
    .reduce((acc, curr) => {
      acc[curr.sampleType] = curr.sum;
      // acc[curr.sampleType] = (curr.percent * 100).toFixed(1);
      return acc;
    }, {});

  sampleTypes.value = summarized;
  sampleTypeColors.value = createPalette(
    Object.keys(summarized),
    palette.value.slice(1)
  );
}

async function getSamplingPeriod() {
  const data = await get("samplingPeriod");
  samplingPeriods.value = data.map(
    (row) =>
      new Object({
        samplingPeriod: row.samplingPeriod.name,
        sum: row._sum.n,
      })
  );
}

async function getSexSummary() {
  const data = await get("sex");
  sexCases.value = data
    .map((row) => new Object({ sex: row.sex.name, sum: row._sum.n }))
    .sort((current, next) => (current.sum > next.sum ? -1 : 1))
    .reduce((acc, curr) => {
      acc[curr.sex] = curr.sum;
      return acc;
    }, {});
}

async function getData() {
  await getResearchCenters();
  await getPrimaryTumors();
  await getSampleType();
  await getSamplingPeriod();
  await getSexSummary();
}

onMounted(() => {
  getData()
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
});
</script>
