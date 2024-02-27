<template>
  <Page>
    <form class="page-section filters-form">
      <fieldset class="page-section-content width-full filters-container">
        <div class="filter-context">
          <legend>Selected Filters:</legend>
        </div>
        <div class="filter-buttons">
          <template
            v-for="key in Object.keys(selectedFilters)"
            v-if="Object.keys(queryFilters.filter).length"
          >
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
        <div class="filter-action">
          <button id="resetFilters" @click="resetFilters">
            <span>Remove all</span>
            <TrashIcon class="heroicons" />
          </button>
          <button id="runQuery" @click="renderCharts">
            <span>Apply Filters</span>
            <ChevronRightIcon class="heroicons" />
          </button>
        </div>
      </fieldset>
    </form>
    <PageSection v-if="error">
      <MessageBox type="error">
        {{ error }}
      </MessageBox>
    </PageSection>
    <Dashboard>
      <DashboardRow :columns="3">
        <DashboardChart>
          <LoadingScreen v-if="loading" />
          <BarChart
            v-else
            chartId="research-centers-sum"
            title="Total number of cases by research center"
            :chartData="researchCenters"
            xvar="_sum"
            yvar="researchCenter"
            :xMax="researchCenterAxis.ymax"
            :xTickValues="researchCenterAxis.ticks"
            yAxisLineBreaker=" "
            :chartHeight="360"
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
              (data) => onChartClick(JSON.parse(data), 'researchCenter')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <DataTable
            tableId="primary-tumors-sum"
            caption="Total number of primary tumors by type"
            :data="primaryTumors"
            :columnOrder="['primaryTumor', 'sum']"
            :enableRowClicks="true"
            @row-clicked="(data) => onChartClick(data, 'primaryTumor')"
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="sample-type-sum"
            title="Total number of samples by type"
            :chartData="sampleTypes"
            :chartColors="sampleTypeColors"
            :chartScale="1"
            :chartHeight="300"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="25"
            legendPosition="bottom"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data: Object) => onChartClick(data, 'sampleType', true)"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <LoadingScreen v-if="loading" />
          <ColumnChart
            v-else
            chartId="sampling-period-sum"
            title="Total number of cases by sampling period"
            :chartData="samplingPeriods"
            xvar="samplingPeriod"
            yvar="_sum"
            :yTickValues="samplingPeriodAxis.ticks"
            :yMax="samplingPeriodAxis.ymax"
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :chartHeight="225"
            :chartMargins="{
              top: 15,
              right: 10,
              bottom: 30,
              left: 60,
            }"
            :enableClicks="true"
            :enableAnimation="true"
            @column-clicked="
              (data) => onChartClick(JSON.parse(data), 'samplingPeriod')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <PieChart2
            chartId="sex-sum"
            title="Number of individuals by sex"
            :chartData="sexCases"
            :chartColors="{
              Male: palette[3],
              Female: palette[5],
            }"
            :chartScale="1"
            :chartHeight="200"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="0"
            legendPosition="top"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data) => onChartClick(data, 'sex', true)"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import {
  Page,
  PageSection,
  MessageBox,
  Dashboard,
  DashboardChart,
  DashboardRow,
  DataTable,
  BarChart,
  PieChart2,
  ColumnChart,
  LoadingScreen,
} from "molgenis-viz";
import {
  MinusCircleIcon,
  ChevronRightIcon,
  TrashIcon,
} from "@heroicons/vue/24/outline";
import { schemeGnBu as scheme } from "d3-scale-chromatic";
import {
  getChartData,
  renameKey,
  createPalette,
  seqAlongBy,
  calculateIncrement,
} from "../utils/index";

const palette = ref(scheme[6]);
let loading = ref(true);
let error = ref(false);

let researchCenters = ref([]);
let researchCenterAxis = ref({ ticks: [], ymax: null });
let primaryTumors = ref([]);
let sampleTypes = ref({});
let samplingPeriods = ref([]);
let samplingPeriodAxis = ref({ ticks: [], ymax: null });
let sexCases = ref({});

let queryFilters = ref({ filter: {} });
let selectedFilters = ref({
  researchCenter: [],
  primaryTumor: [],
  sampleType: [],
  samplingPeriod: [],
  sex: [],
});

function removeFilter(key: string, value: String) {
  selectedFilters.value[key] = selectedFilters.value[key].filter(
    (q: String) => q !== value
  );
  updateQueryFilters();
  renderCharts();
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

        subfilters.forEach((value: String) => {
          const newSubFilter = {};
          newSubFilter[key] = { name: { equals: value } };
          query["_or"].push(newSubFilter);
        });
      }
    }
  }

  queryFilters.value.filter = query;
}

let sampleTypeColors = computed(() => {
  return createPalette(Object.keys(sampleTypes.value), palette.value.slice(1));
});

async function getAllData() {
  researchCenters.value = await getChartData({
    labels: "researchCenter",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  researchCenters.value = researchCenters.value
    .sort((curr, next) => curr._sum - next._sum)
    .reverse();

  const centerMax = Math.max(...researchCenters.value.map((d) => d._sum));
  const centerStep = calculateIncrement(centerMax);
  researchCenterAxis.value.ymax =
    Math.ceil(centerMax / centerStep) * centerStep;
  researchCenterAxis.value.ticks = seqAlongBy(
    0,
    researchCenterAxis.value.ymax,
    centerStep
  );

  primaryTumors.value = await getChartData({
    labels: "primaryTumor",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  renameKey(primaryTumors.value, "_sum", "sum");

  sampleTypes.value = await getChartData({
    labels: "sampleType",
    values: "_sum",
    filters: queryFilters.value.filter,
    asPieChartData: true,
  });

  samplingPeriods.value = await getChartData({
    labels: "samplingPeriod",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  const maxValue = Math.max(...samplingPeriods.value.map((d) => d._sum));
  const step = calculateIncrement(maxValue);
  const max = Math.ceil(maxValue / step) * step;
  samplingPeriodAxis.value.ymax = max;
  samplingPeriodAxis.value.ticks = seqAlongBy(0, max, step);

  const ordering = [
    "<1991",
    "1991-2000",
    "2001-2005",
    "2006-2010",
    "2011-2015",
    "2016-2020",
    "2021-2025",
  ];

  samplingPeriods.value = samplingPeriods.value.sort((current, next) => {
    return (
      ordering.indexOf(current.samplingPeriod) -
      ordering.indexOf(next.samplingPeriod)
    );
  });

  sexCases.value = await getChartData({
    labels: "sex",
    values: "_sum",
    asPieChartData: true,
    filters: queryFilters.value.filter,
  });
}

function resetFilters() {
  selectedFilters.value = {
    researchCenter: [],
    primaryTumor: [],
    sampleType: [],
    samplingPeriod: [],
    sex: [],
  };
  updateQueryFilters();
  renderCharts();
}

function onChartClick(
  data: Object | String,
  attribute: string,
  isPieChart: Boolean = false
) {
  const value = isPieChart ? Object.keys(data)[0] : data[attribute];
  if (selectedFilters.value[attribute].indexOf(value) === -1) {
    selectedFilters.value[attribute].push(value);
    updateQueryFilters();
  }
}

function renderCharts() {
  loading.value = true;
  getAllData()
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
}

onMounted(() => renderCharts());
</script>
