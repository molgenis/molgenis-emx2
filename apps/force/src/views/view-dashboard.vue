<template>
  <Page>
    <PageHeader title="FORCE Project" subtitle="Search and explore the data" />
    <PageSection width="large">
      <h2>Filters</h2>
      <p>
        All of charts are interactive and connected. By clicking any of the
        elements, you can create a new filter, which will update all of the
        charts. Click on a filter button below to remove it.
      </p>
    </PageSection>
    <form class="page-section filters-form">
      <fieldset class="page-section-content width-full filters-container">
        <legend>Selected Filters</legend>
        {{ queryFilters.filter }}
        <div
          class="filter-buttons"
          v-if="Object.keys(queryFilters.filter).length"
        >
          <template v-for="key in Object.keys(selectedFilters)">
            <div class="filter-button" v-for="value in selectedFilters[key]">
              <p>{{ value }}</p>
              <button
                :id="`filter-${key}-${value}`"
                @click="removeFilter(key, value)"
              >
                <span class="visually-hidden">remove {{ value }}</span>
                <MinusCircleIcon class="heroicons" />
              </button>
            </div>
          </template>
        </div>
        <div v-else>
          <p>No filters applied.</p>
        </div>
      </fieldset>
    </form>
    <PageSection v-if="error">
      <MessageBox type="error">
        {{ error }}
      </MessageBox>
    </PageSection>
    <Dashboard v-if="!loading && !error">
      <DashboardRow :columns="3">
        <DashboardChart>
          <ColumnChart
            chartId="research-centers-sum"
            title="Sum of cases by research center"
            :chartData="researchCenters"
            xvar="researchCenter"
            yvar="sum"
            xAxisLineBreaker=" "
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :columnPaddingOuter="0"
            :chartHeight="350"
            :chartMargins="{
              top: 10,
              right: 35,
              bottom: 40,
              left: 60,
            }"
            :enableClicks="true"
            @column-clicked="
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
            title="Sum of cases by sampling period"
            :chartData="samplingPeriods"
            xvar="samplingPeriod"
            yvar="sum"
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :chartHeight="210"
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
import { GraphQLClient, gql } from "graphql-request";
import { ref, onMounted } from "vue";

import {
  Page,
  PageSection,
  MessageBox,
  Dashboard,
  DashboardChart,
  DashboardRow,
  PageHeader,
  BarChart,
  ColumnChart,
  DataTable,
  PieChart2,
} from "molgenis-viz";

import { MinusCircleIcon } from "@heroicons/vue/24/outline";
import { schemePuBu as scheme } from "d3-scale-chromatic";
import { createPalette } from "../utils/index";

const client = new GraphQLClient("../api/graphql");

const palette = ref(scheme[6]);

let loading = ref(true);
let error = ref(false);
let researchCenters = ref([]);
let primaryTumors = ref([]);
let sampleTypes = ref({});
let samplingPeriods = ref([]);
let sexCases = ref({});
let sampleTypeColors = ref({});
let selectedFilters = ref({
  researchCenter: [],
  primaryTumor: [],
  sampleType: [],
  samplingPeriod: [],
  sex: [],
});

let queryFilters = ref({ filter: {} });

function removeFilter(key, value) {
  selectedFilters.value[key] = selectedFilters.value[key].filter(
    (q) => q !== value
  );
  updateQueryFilters();
  getAllData();
}

function updateQueryFilters() {
  const query = {};
  const filterKeys = Object.keys(selectedFilters.value);
  const filterLength = filterKeys.length;

  for (let i = 0; i < filterLength; i++) {
    const key = filterKeys[i];
    const subfilters = selectedFilters.value[key];

    if (typeof subfilters[0] !== "undefined") {
      if (subfilters.length === 1) {
        query[key] = { name: { equals: subfilters[0] } };
      }

      if (subfilters.length > 1) {
        if (Object.keys(query).indexOf("_or")) {
          query["_or"] = [];
        }

        subfilters.forEach((value) => {
          const newSubFilter = {};
          newSubFilter[key] = { name: { equals: value } };
          query["_or"].push(newSubFilter);
        });
      }
    }
  }

  queryFilters.value.filter = query;
}

async function getData(attribute) {
  const query = gql` query ($filters: ClinicalDataFilter ) {
    ClinicalData_groupBy ( filter: $filters ) {
      ${attribute} {
        name
      }
      _sum {
        n
      }
    }
  }`;
  const variables = { filters: queryFilters.value.filter };
  const response = await client.request(query, variables);
  return response.ClinicalData_groupBy;
}

function onChartClick(data, attribute) {
  const value = data[attribute];
  if (selectedFilters.value[attribute].indexOf(value) === -1) {
    selectedFilters.value[attribute].push(value);
    updateQueryFilters();
    getAllData();
  }
}

function onPieChartClick(data, attribute) {
  const value = Object.keys(data)[0];
  if (selectedFilters.value[attribute].indexOf(value) === -1) {
    selectedFilters.value[attribute].push(value);
    updateQueryFilters();
    getAllData();
  }
}

async function getResearchCenters() {
  const data = await getData("researchCenter");
  researchCenters.value = data
    .map((row) => {
      return {
        researchCenter: row.researchCenter.name,
        sum: row._sum.n,
      };
    })
    .sort((current, next) => {
      return current.sum > next.sum ? -1 : 1;
    });
}

async function getPrimaryTumors() {
  const data = await getData("primaryTumor");
  primaryTumors.value = data
    .map((row) => {
      return {
        primaryTumor: row.primaryTumor.name,
        sum: row._sum.n,
      };
    })
    .sort((current, next) => {
      return current.primaryTumor > next.primaryTumor ? 1 : -1;
    });
}

async function getSampleType() {
  const data = await getData("sampleType");
  const total = data.reduce((sum, curr) => curr._sum.n + sum, 0);
  const summarized = data
    .map((row) => {
      return {
        sampleType: row.sampleType.name,
        sum: row._sum.n,
        percent: ((row._sum.n / total) * 100).toFixed(1),
      };
    })
    .sort((current, next) => (current.sum > next.sum ? -1 : 1))
    .reduce((acc, curr) => {
      acc[curr.sampleType] = curr.sum;
      return acc;
    }, {});

  sampleTypes.value = summarized;
  sampleTypeColors.value = createPalette(
    Object.keys(summarized),
    palette.value.slice(1)
  );
}

async function getSamplingPeriod() {
  const data = await getData("samplingPeriod");
  samplingPeriods.value = data.map((row) => {
    return {
      samplingPeriod: row.samplingPeriod.name,
      sum: row._sum.n,
    };
  });
}

async function getSexSummary() {
  const data = await getData("sex");
  sexCases.value = data
    .map((row) => {
      return {
        sex: row.sex.name,
        sum: row._sum.n,
      };
    })
    .sort((current, next) => (current.sum > next.sum ? -1 : 1))
    .reduce((acc, curr) => {
      acc[curr.sex] = curr.sum;
      return acc;
    }, {});
}

async function getAllData() {
  await getResearchCenters();
  await getPrimaryTumors();
  await getSampleType();
  await getSamplingPeriod();
  await getSexSummary();
}

onMounted(() => {
  getAllData()
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
});
</script>
