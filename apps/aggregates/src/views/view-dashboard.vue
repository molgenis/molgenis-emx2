<template>
  <Page>
    <form class="page-section filters-form">
      <fieldset class="page-section-content width-full filters-container">
        <div class="filter-item selected-filters">
          <div class="filter-context">
            <legend>Selected Filters</legend>
          </div>
          <div class="filter-buttons">
            <template
              v-for="key in Object.keys(selectedFilters)"
              v-if="Object.keys(queryFilters.filter).length"
            >
              <div
                class="filter-button"
                v-for="value in selectedFilters[key as keyof selectedFiltersIF]"
              >
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
        </div>
        <div class="filter-item filter-action">
          <button
            id="resetFilters"
            @click="resetFilters"
            @click.prevent="onClickPrevent"
          >
            <span>Remove all</span>
            <TrashIcon class="heroicons" />
          </button>
          <button
            id="runQuery"
            @click="renderCharts"
            @click.prevent="onClickPrevent"
          >
            <span>Apply Filters</span>
            <ChevronRightIcon class="heroicons" />
          </button>
        </div>
      </fieldset>
      <Accordion title="How to use this dashboard" id="dashboard-instructions">
        <p>
          To filter data, click on one or more elements in the charts below. For
          example, a row in a table or a column in a bar chart. Filters will
          appear in the "selected filters" list below. When you are satisfied
          with your selection, click the "Apply Filters" button to update the
          charts. Click the "remove all" button to clear all filters and reset
          the charts. A filter can also be removed by clicking the "remove icon"
          (<MinusCircleIcon />). Doing so will automatically update the charts.
        </p>
      </Accordion>
    </form>
    <PageSection v-if="error">
      <MessageBox type="error">
        {{ error }}
      </MessageBox>
    </PageSection>
    <Dashboard>
      <DashboardRow class="dashboard-meta" :columns="1">
        <p class="resource-total-cases">
          Total number of cases: {{ totalNumberOfCases }}
        </p>
      </DashboardRow>
      <DashboardRow :columns="3">
        <DashboardChart>
          <LoadingScreen style="height: 400px" v-if="loading" />
          <BarChart
            v-else
            chartId="chart-number-of-cases-by-research-center"
            title="Number of cases by research center"
            :chartData="researchCenters"
            xvar="_sum"
            yvar="researchCenter"
            :xMax="researchCenterAxis.ymax"
            :xTickValues="researchCenterAxis.ticks"
            yAxisLineBreaker=" "
            :chartHeight="400"
            :barFill="palette[3]"
            :barHoverFill="palette[5]"
            :chartMargins="{
              top: 10,
              right: 40,
              bottom: 30,
              left: 100,
            }"
            :enableClicks="true"
            :enableAnimation="true"
            @bar-clicked="
              (data: string) => onChartClick(JSON.parse(data), 'researchCenter')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen style="height: 100%" v-if="loading" />
          <DataTable
            v-else
            tableId="chart-number-of-cases-by-primary-tumor"
            caption="Number of cases by primary tumor"
            :data="primaryTumorSite"
            :columnOrder="['primary tumor site', 'sum']"
            :enableRowClicks="true"
            @row-clicked="(data: object) => onChartClick(data, 'primaryTumorSite')"
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen style="height: 100%" v-if="loading" />
          <PieChart2
            v-else
            chartId="chart-number-cases-by-stage-at-diagnosis"
            title="Number of cases by stage at diagnosis, distant metastasis"
            :chartData="metastasis"
            :chartScale="1"
            :chartHeight="350"
            :chartColors="{
              No: palette[3],
              Yes: palette[5],
              Unknown: palette[2],
            }"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="25"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data: object) => onChartClick(data, 'metastasis', true)"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow class="dashboard-row-offset" :columns="2">
        <DashboardChart>
          <LoadingScreen style="height: 300px" v-if="loading" />
          <ColumnChart
            v-else
            chartId="chart-number-of-cases-by-year-of-diagnosis"
            title="Number of cases by year of diagnosis"
            :chartData="yearOfDiagnosis"
            xvar="yearOfDiagnosis"
            yvar="_sum"
            :yTickValues="yearOfDiagnosisAxis.ticks"
            :yMax="yearOfDiagnosisAxis.ymax"
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :chartHeight="250"
            :chartMargins="{
              top: 15,
              right: 10,
              bottom: 30,
              left: 60,
            }"
            :enableClicks="true"
            :enableAnimation="true"
            @column-clicked="
              (data: string) => onChartClick(JSON.parse(data), 'yearOfDiagnosis')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen style="height: 100%" v-if="loading" />
          <PieChart2
            v-else
            chartId="chart-number-of-cases-by-sex"
            title="Number of cases by sex"
            :chartData="sexCases"
            :chartColors="{
              Male: palette[3],
              Female: palette[5],
            }"
            :chartScale="1"
            :chartHeight="250"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="0"
            legendPosition="top"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data: object) => onChartClick(data, 'sex', true)"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from "vue";

// @ts-ignore
// prettier-ignore
import { Page, PageSection, Accordion, MessageBox, Dashboard, DashboardChart, DashboardRow, DataTable, BarChart, PieChart2, ColumnChart, LoadingScreen } from "molgenis-viz";

import {
  MinusCircleIcon,
  ChevronRightIcon,
  TrashIcon,
} from "@heroicons/vue/24/outline";
import { schemeGnBu as scheme } from "d3-scale-chromatic";
import {
  getChartData,
  renameKey,
  seqAlongBy,
  calculateIncrement,
  gqlPrepareSubSelectionFilter,
} from "../utils/index";

import type {
  selectedFiltersIF,
  researchCentersIF,
  primaryTumorSiteIF,
  metastasisIF,
  yearOfDiagnosisIF,
  sexCasesIF,
  chartAxisSettingsIF,
  selectedFiltersQueryIF,
  vizChartFilters,
} from "../interfaces/types";

const palette = ref(scheme[6]);
const loading = ref(true);
const error = ref(false);

const researchCenters = ref<researchCentersIF[]>([]);
const primaryTumorSite = ref<primaryTumorSiteIF[]>([]);
const metastasis = ref<metastasisIF[]>([]);
const yearOfDiagnosis = ref<yearOfDiagnosisIF[]>([]);
const sexCases = ref<sexCasesIF>();
const researchCenterAxis = ref<chartAxisSettingsIF>({ ticks: [], ymax: null });
const yearOfDiagnosisAxis = ref<chartAxisSettingsIF>({ ticks: [], ymax: null });

const queryFilters = ref({ filter: {} });
const selectedFilters = ref<selectedFiltersIF>({
  researchCenter: [],
  primaryTumorSite: [],
  metastasis: [],
  yearOfDiagnosis: [],
  sex: [],
});

const totalNumberOfCases = computed(() => {
  return researchCenters.value.reduce(
    (sum: number, row: Record<string, any>) => row._sum + sum,
    0
  );
});

function onClickPrevent(event: Event) {
  event.preventDefault();
}

function removeFilter(key: string, value: String) {
  selectedFilters.value[key as keyof selectedFiltersIF] = selectedFilters.value[
    key as keyof selectedFiltersIF
  ].filter((q: String) => q !== value);
  updateQueryFilters();
  renderCharts();
}

function updateQueryFilters() {
  const query: vizChartFilters = {};
  const filterKeys = Object.keys(selectedFilters.value);
  const filterLength = filterKeys.length;

  for (let i = 0; i < filterLength; i++) {
    const key = filterKeys[i];
    const gqlKey = key === "researchCenter" ? "displayName" : "name";
    const gqlAction = key === "researchCenter" ? "equals" : "equals";
    const subfilters = selectedFilters.value[key as keyof selectedFiltersIF];

    if (typeof subfilters[0] !== "undefined") {
      if (subfilters.length === 1) {
        query[key as keyof selectedFiltersQueryIF] =
          gqlPrepareSubSelectionFilter(gqlKey, subfilters[0], gqlAction);
      }

      if (subfilters.length > 1) {
        if (Object.keys(query).indexOf("_or")) {
          query._or = [];
        }

        subfilters.forEach((value: string) => {
          const newSubFilter: selectedFiltersQueryIF = {};
          newSubFilter[key as keyof selectedFiltersQueryIF] =
            gqlPrepareSubSelectionFilter(gqlKey, value, gqlAction);
          query._or?.push(newSubFilter);
        });
      }
    }
  }

  queryFilters.value.filter = query;
}

async function getResearchCentersData() {
  researchCenters.value = await getChartData({
    table: "FORCE_NENPatients",
    sub_attribute: "displayName",
    labels: "researchCenter",
    nestedLabelKey: "displayName",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  researchCenters.value = researchCenters.value
    .sort((curr, next) => curr._sum - next._sum)
    .reverse();
}

async function getPrimaryTumorSiteData() {
  primaryTumorSite.value = await getChartData({
    table: "FORCE_NENPatients",
    sub_attribute: "name",
    labels: "primaryTumorSite",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  primaryTumorSite.value = primaryTumorSite.value.map((row) => {
    return { ...row, "primary tumor site": row.primaryTumorSite };
  });
  renameKey(primaryTumorSite.value, "_sum", "sum");
}

async function getMetastasisData() {
  metastasis.value = await getChartData({
    table: "FORCE_NENPatients",
    sub_attribute: "name",
    labels: "metastasis",
    values: "_sum",
    filters: queryFilters.value.filter,
    asPieChartData: true,
  });
}

async function getYearofDiagnosisData() {
  yearOfDiagnosis.value = await getChartData({
    table: "FORCE_NENPatients",
    sub_attribute: "name",
    labels: "yearOfDiagnosis",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  const ordering = [
    "<1991",
    "1991-2000",
    "2001-2005",
    "2006-2010",
    "2011-2015",
    "2016-2020",
    "2021-2025",
  ];

  yearOfDiagnosis.value = yearOfDiagnosis.value.sort(
    (current: Record<string, any>, next: Record<string, any>) => {
      return (
        ordering.indexOf(current.yearOfDiagnosis) -
        ordering.indexOf(next.yearOfDiagnosis)
      );
    }
  );
}

async function getSexData() {
  sexCases.value = await getChartData({
    table: "FORCE_NENPatients",
    sub_attribute: "name",
    labels: "sex",
    values: "_sum",
    asPieChartData: true,
    filters: queryFilters.value.filter,
  });
}

watch([researchCenters], () => {
  const centerMax = Math.max(...researchCenters.value.map((d) => d._sum));
  const centerStep = calculateIncrement(centerMax);
  researchCenterAxis.value.ymax =
    Math.ceil(centerMax / centerStep) * centerStep;
  researchCenterAxis.value.ticks = seqAlongBy(
    0,
    researchCenterAxis.value.ymax,
    centerStep
  );
});

watch([yearOfDiagnosis], () => {
  const allMaxValues: number[] = yearOfDiagnosis.value.map(
    (row: Record<string, any>) => row._sum
  );
  const maxValue = Math.max(...allMaxValues);
  const step = calculateIncrement(maxValue);
  const max = Math.ceil(maxValue / step) * step;
  yearOfDiagnosisAxis.value.ymax = max;
  yearOfDiagnosisAxis.value.ticks = seqAlongBy(0, max, step);
});

function resetFilters() {
  selectedFilters.value = {
    researchCenter: [],
    primaryTumorSite: [],
    metastasis: [],
    yearOfDiagnosis: [],
    sex: [],
  };
  updateQueryFilters();
  renderCharts();
}

function onChartClick(
  data: Record<string, any>,
  attribute: string,
  isPieChart: boolean = false
) {
  const value = isPieChart
    ? Object.keys(data)[0]
    : Object.hasOwn(data, attribute)
    ? data[attribute]
    : data;
  if (
    selectedFilters.value[attribute as keyof selectedFiltersIF].indexOf(
      value
    ) === -1
  ) {
    selectedFilters.value[attribute as keyof selectedFiltersIF].push(value);
    updateQueryFilters();
  }
}

function renderCharts() {
  loading.value = true;
  Promise.all([
    getResearchCentersData(),
    getPrimaryTumorSiteData(),
    getMetastasisData(),
    getYearofDiagnosisData(),
    getSexData(),
  ])
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
}

onMounted(() => renderCharts());
</script>
