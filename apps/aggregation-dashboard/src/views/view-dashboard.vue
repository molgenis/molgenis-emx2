<template>
  <Page>
    <PageHeader
      title="Aggregation Dashboard"
      subtitle="Explore collections in the catalogue"
      height="medium"
      imageSrc="aggregation-header.jpg"
    />
    <form class="page-section filters-form">
      <fieldset class="page-section-content width-full filters-container">
        <div class="filter-item selected-resources">
          <label>Choose a resource</label>
          <select
            id="resource-selection"
            @change="(event) => onChartClick(event.target.value, 'resource')"
            ref="resourcesInput"
          >
            <option value="" disabled selected>Resources</option>
            <option v-for="resource in resources" :value="resource.resource">
              {{ resource.resource }}
            </option>
          </select>
        </div>
        <div class="filter-item selected-filters">
          <div class="filter-context">
            <legend>Selected Filters</legend>
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
    </form>
    <PageSection v-if="error">
      <MessageBox type="error">
        {{ error }}
      </MessageBox>
    </PageSection>
    <Dashboard>
      <DashboardRow :columns="3">
        <DashboardChart>
          <LoadingScreen style="height: 350px;" v-if="loading" />
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
              left: 75,
            }"
            :enableClicks="true"
            :enableAnimation="true"
            @bar-clicked="
              (data) => onChartClick(JSON.parse(data), 'researchCenter')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen v-if="loading" />
          <DataTable
            v-else
            tableId="primary-tumors-sum"
            caption="Total number of primary tumors by type"
            :data="primaryTumorSite"
            :columnOrder="['primaryTumorSite', 'sum']"
            :enableRowClicks="true"
            @row-clicked="(data) => onChartClick(data, 'primaryTumorSite')"
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen v-if="loading" />
          <PieChart2
            v-else
            chartId="metastatis-sum"
            title="Total Number of Metastasis by type"
            :chartData="metastasis"
            :chartScale="1"
            :chartHeight="300"
            :chartColors="{
              No: palette[3],
              Yes: palette[5],
              Unknown: palette[2],
            }"
            :asDonutChart="true"
            :enableLegendHovering="true"
            :chartMargins="25"
            legendPosition="bottom"
            :valuesArePercents="false"
            :enableClicks="true"
            @slice-clicked="(data: Object) => onChartClick(data, 'metastasis', true)"
          />
        </DashboardChart>
      </DashboardRow>
      <DashboardRow :columns="2">
        <DashboardChart>
          <LoadingScreen style="height: 250px;" v-if="loading" />
          <ColumnChart
            v-else
            chartId="sampling-period-sum"
            title="Total number of cases by sampling period"
            :chartData="yearOfDiagnosis"
            xvar="yearOfDiagnosis"
            yvar="_sum"
            :yTickValues="yearOfDiagnosisAxis.ticks"
            :yMax="yearOfDiagnosisAxis.ymax"
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
              (data) => onChartClick(JSON.parse(data), 'yearOfDiagnosis')
            "
          />
        </DashboardChart>
        <DashboardChart>
          <LoadingScreen style="height: 250px;" v-if="loading" />
          <PieChart2
            v-else
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
  PageHeader,
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
const loading = ref(true);
const error = ref(false);

const resources = ref([]);
const researchCenters = ref([]);
const researchCenterAxis = ref({ ticks: [], ymax: null });
const primaryTumorSite = ref([]);
const metastasis = ref([]);
const yearOfDiagnosis = ref([]);
const yearOfDiagnosisAxis = ref({ ticks: [], ymax: null });
const sexCases = ref({});

const resourcesInput = ref();

const queryFilters = ref({ filter: {} });
const selectedFilters = ref({
  resource: [],
  researchCenter: [],
  primaryTumorSite: [],
  metastasis: [],
  yearOfDiagnosis: [],
  sex: [],
});

function onClickPrevent(event: Event) {
  event.preventDefault();
}

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

async function getAllData() {
  resources.value = await getChartData({
    labels: "resource",
    value: "_sum",
  });

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

  primaryTumorSite.value = await getChartData({
    labels: "primaryTumorSite",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  renameKey(primaryTumorSite.value, "_sum", "sum");

  metastasis.value = await getChartData({
    labels: "metastasis",
    values: "_sum",
    filters: queryFilters.value.filter,
    asPieChartData: true,
  });

  yearOfDiagnosis.value = await getChartData({
    labels: "yearOfDiagnosis",
    values: "_sum",
    filters: queryFilters.value.filter,
  });

  const maxValue = Math.max(...yearOfDiagnosis.value.map((d) => d._sum));
  const step = calculateIncrement(maxValue);
  const max = Math.ceil(maxValue / step) * step;
  yearOfDiagnosisAxis.value.ymax = max;
  yearOfDiagnosisAxis.value.ticks = seqAlongBy(0, max, step);

  const ordering = [
    "1991-2000",
    "2001-2005",
    "2006-2010",
    "2011-2015",
    "2016-2020",
    "2021-2025",
  ];

  yearOfDiagnosis.value = yearOfDiagnosis.value.sort((current, next) => {
    return (
      ordering.indexOf(current.yearOfDiagnosis) -
      ordering.indexOf(next.yearOfDiagnosis)
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
    resource: [],
    researchCenter: [],
    primaryTumor: [],
    metastasis: [],
    yearOfDiagnosis: [],
    sex: [],
  };
  resourcesInput.value = "";
  updateQueryFilters();
  renderCharts();
}

function onChartClick(
  data: Object | String,
  attribute: string,
  isPieChart: Boolean = false
) {
  const value = isPieChart
    ? Object.keys(data)[0]
    : Object.hasOwn(data, attribute)
    ? data[attribute]
    : data;
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
