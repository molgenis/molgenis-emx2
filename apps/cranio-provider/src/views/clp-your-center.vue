<template>
  <ProviderDashboard>
    <LoadingBlock :loading="loading">
      <h2 class="dashboard-h2">
        Overview of patients {{ selectedAgeGroup }} of age (n={{
          totalNumberOfPatients
        }})
      </h2>
    </LoadingBlock>
    <DashboardRow :columns="1">
      <DashboardChart>
        <h3>Options</h3>
        <InputLabel
          id="yearOfBirthFilter"
          label="Year of birth"
          description="Limit the results by year of birth"
        />
        <select
          class="inputs select"
          id="yearOfBirthFilter"
          v-model="selectedAgeGroup"
          @change="updateCharts"
        >
          <option v-for="ageGroup in ageGroups" :value="ageGroup">
            {{ ageGroup }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart id="clp-patients-by-phenotype">
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="patientsByPhenotypeChart?.chartId"
          :title="patientsByPhenotypeChart?.chartTitle"
          :description="patientsByPhenotypeChart?.chartSubtitle"
          :chartData="patientsByPhenotypeChartData"
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="patientsByPhenotypeChart?.xAxisLabel"
          :yAxisLabel="patientsByPhenotypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="patientsByPhenotypeChart?.yAxisMaxValue"
          :yTickValues="patientsByPhenotypeChart?.yAxisTicks"
          :columnColorPalette="patientsByPhenotypePalette"
          columnHoverFill="#708fb4"
          :chartHeight="250"
          :chartMargins="{
            top: patientsByPhenotypeChart?.topMargin,
            right: patientsByPhenotypeChart?.rightMargin,
            bottom: patientsByPhenotypeChart?.bottomMargin,
            left: patientsByPhenotypeChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <PieChart2
          v-else
          :chartId="patientsByGenderChart?.chartId"
          :title="patientsByGenderChart?.chartTitle"
          :description="patientsByGenderChart?.chartSubtitle"
          :chartData="patientsByGenderChartData"
          :chartColors="patientsByGenderPalette"
          :valuesArePercents="false"
          :asDonutChart="true"
          :enableLegendHovering="true"
          legendPosition="bottom"
          :enableClicks="true"
          :chartHeight="215"
          :chartScale="0.85"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart v-if="!loading && currentVisibleMeter === 'cleftq'">
        <ProgressMeter
          :chartId="cleftqCompletionChart?.chartId"
          :title="`% of patients that completed the CLEFT-Q (${selectedAgeGroup})`"
          :value="progressMeterValue"
          :totalValue="100"
          :barHeight="25"
          barFill="#66c2a4"
        />
      </DashboardChart>
      <DashboardChart v-if="!loading && currentVisibleMeter === 'ics'">
        <ProgressMeter
          :chartId="icsCompletionChart?.chartId"
          :title="`% of patients that completed the ICS (${selectedAgeGroup})`"
          :value="progressMeterValue"
          :totalValue="100"
          :barHeight="25"
          barFill="#9f6491"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  PieChart2,
  ProgressMeter,
  InputLabel,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";
import LoadingBlock from "../components/LoadingBlock.vue";
import ProviderDashboard from "../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../utils/generateAxisTicks";
import { asKeyValuePairs, sum, uniqueValues } from "../utils";
import { generateColorPalette } from "../utils/generateColorPalette";
import { getDashboardChart } from "../utils/getDashboardData";
import { getUniqueAgeRanges } from "../utils/clpUtils";

import type { ICharts, IChartData } from "../types/schema";
import type { IAppPage } from "../types/app";
import type { IKeyValuePair, clpChartTypes } from "../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const ageGroups = ref<string[]>();
const selectedAgeGroup = ref<string>();
const totalNumberOfPatients = ref<number>(0);
const patientsByPhenotypeChart = ref<ICharts>();
const patientsByPhenotypeChartData = ref<IChartData[]>();
const patientsByPhenotypePalette = ref<IKeyValuePair>();
const patientsByGenderChart = ref<ICharts>();
const patientsByGenderChartData = ref<IKeyValuePair>();
const patientsByGenderPalette = ref<IKeyValuePair>();
const icsCompletionChart = ref<ICharts>();
const cleftqCompletionChart = ref<ICharts>();
const progressMeterValue = ref<number>(0);

const currentVisibleMeter = computed<clpChartTypes>(() => {
  if (["3-4 years", "5-6 years"].includes(selectedAgeGroup.value as string)) {
    return "ics";
  } else {
    return "cleftq";
  }
});

async function getPageData() {
  const phenotypesResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-patients-by-phenotype"
  );

  const genderResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-patients-by-gender"
  );

  const icsResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-ics-completed"
  );

  const cleftqResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-cleft-q-completed"
  );

  patientsByPhenotypeChart.value = phenotypesResponse[0];
  patientsByGenderChart.value = genderResponse[0];
  icsCompletionChart.value = icsResponse[0];
  cleftqCompletionChart.value = cleftqResponse[0];
}

function updatePhenotypesChart() {
  patientsByPhenotypeChartData.value =
    patientsByPhenotypeChart.value?.dataPoints
      ?.filter((row: IChartData) => {
        return row.dataPointPrimaryCategory === selectedAgeGroup.value;
      })
      .sort((a: IChartData, b: IChartData) => {
        return (a.dataPointOrder as number) - (b.dataPointOrder as number);
      });

  const chartTicks = generateAxisTickData(
    patientsByPhenotypeChartData.value!,
    "dataPointValue"
  );
  if (patientsByPhenotypeChart.value) {
    patientsByPhenotypeChart.value.yAxisMaxValue = chartTicks.limit;
    patientsByPhenotypeChart.value.yAxisTicks = chartTicks.ticks;
  }

  totalNumberOfPatients.value = sum(
    patientsByPhenotypeChartData.value,
    "dataPointValue"
  );
}

function updateGenderChart() {
  const filteredData = patientsByGenderChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedAgeGroup.value;
    })
    .sort((a: IChartData, b: IChartData): number => {
      return (b.dataPointValue as number) - (a.dataPointValue as number);
    });

  patientsByGenderChartData.value = asKeyValuePairs(
    filteredData,
    "dataPointName",
    "dataPointValue"
  );
}

function updateProgressMeter() {
  console.log(currentVisibleMeter.value, selectedAgeGroup.value);
  if (currentVisibleMeter.value === "ics") {
    const icsFiltered = icsCompletionChart.value?.dataPoints?.filter(
      (row: IChartData) => {
        return row.dataPointPrimaryCategory === selectedAgeGroup.value;
      }
    ) as IChartData[];
    if (icsFiltered[0].dataPointValue) {
      progressMeterValue.value = icsFiltered[0].dataPointValue;
    }
  } else {
    const cleftqFilterd = cleftqCompletionChart.value?.dataPoints?.filter(
      (row: IChartData) => {
        return row.dataPointPrimaryCategory === selectedAgeGroup.value;
      }
    ) as IChartData[];
    if (cleftqFilterd[0].dataPointValue) {
      progressMeterValue.value = cleftqFilterd[0].dataPointValue;
    }
  }
}

function updateCharts() {
  updatePhenotypesChart();
  updateGenderChart();
  updateProgressMeter();
}

onMounted(() => {
  getPageData()
    .then(() => {
      ageGroups.value = getUniqueAgeRanges(
        patientsByPhenotypeChart.value?.dataPoints!,
        "dataPointPrimaryCategory"
      );
      selectedAgeGroup.value = ageGroups.value[0];

      const phenotypeCategories = uniqueValues(
        patientsByPhenotypeChart.value?.dataPoints,
        "dataPointName"
      );
      patientsByPhenotypePalette.value =
        generateColorPalette(phenotypeCategories);

      const genderCategories = uniqueValues(
        patientsByGenderChart.value?.dataPoints,
        "dataPointName"
      );
      patientsByGenderPalette.value = generateColorPalette(genderCategories);
    })
    .then(() => {
      updateCharts();
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>
