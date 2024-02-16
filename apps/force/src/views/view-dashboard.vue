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
    <Dashboard>
      <DashboardRow :columns="3">
        <DashboardChart>
          <DataTable
            tableId="research-centers-sum"
            caption="Sum of cases by research center"
            :data="researchCenters"
            :columnOrder="['researchCenter', 'sum']"
            :enableClicks="true"
            @row-clicked="(data) => onChartClick(data, 'researchCenter')"
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
            :chartScale="1"
            :chartHeight="225"
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
          <LoadingScreen v-if="loading" style="width:100%; height:100%;" />
          <ColumnChart
            v-else
            chartId="sampling-period-sum"
            title="Sum of cases by sampling period"
            :chartData="samplingPeriods"
            xvar="samplingPeriod"
            yvar="_sum"
            :yTickValues="samplingPeriodAxis.ticks"
            :yMax="samplingPeriodAxis.ymax"
            :columnFill="palette[5]"
            :columnHoverFill="palette[3]"
            :chartHeight="210"
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
  PageHeader,
  DataTable,
  PieChart2,
  ColumnChart,
  LoadingScreen,
} from "molgenis-viz";
import { MinusCircleIcon } from "@heroicons/vue/24/outline";
import { schemePuBu as scheme } from "d3-scale-chromatic";
import { getChartData, renameKey, createPalette, seqAlongBy, calculateIncrement } from "../utils/index";

const palette = ref(scheme[6]);
let loading = ref(true);
let error = ref(false);

let researchCenters = ref([]);
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

  renameKey(researchCenters.value, "_sum", "sum");

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
  
  const maxValue = Math.max(...samplingPeriods.value.map(d => d._sum));
  const step = calculateIncrement(maxValue);
  const max = Math.ceil(maxValue / step) * step;
  samplingPeriodAxis.value.ymax = max
  samplingPeriodAxis.value.ticks = seqAlongBy(0, max, step)

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

function onChartClick(
  data: Object | String,
  attribute: string,
  isPieChart: Boolean = false
) {
  const value = isPieChart ? Object.keys(data)[0] : data[attribute];
  if (selectedFilters.value[attribute].indexOf(value) === -1) {
    selectedFilters.value[attribute].push(value);
    updateQueryFilters();
    renderCharts();
  }
}

function renderCharts () {
  loading.value = true;
  getAllData()
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
  
}

onMounted(() => {
  renderCharts();
});
</script>
