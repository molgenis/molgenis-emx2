<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">Overview of Cleft lip and palate patients</h2>
    <DashboardRow :columns="1">
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
    </DashboardRow>
    <LoadingBlock :loading="loading">
      <h2 class="dashboard-h2">
        Overview of {{ selectedCleftType }} patients (n={{
          selectedCleftTypePatients
        }})
      </h2>
    </LoadingBlock>
    <DashboardRow :columns="1">
      <DashboardChart>
        <h3 class="visually-hidden">Options</h3>
        <InputLabel
          id="clpYourCentreOverviewFilter"
          label="Select cleft type"
        />
        <select
          class="inputs select"
          id="clpYourCentreOverviewFilter"
          v-model="selectedCleftType"
          @change="updateCharts"
        >
          <option v-for="cleftType in cleftTypeOptions" :value="cleftType">
            {{ cleftType }}
          </option>
        </select>
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
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
      <LoadingScreen v-if="loading" style="height: 100%" />
      <template v-else>
        <DashboardChart>
          <h3 class="dashboard-h3" style="margin-bottom: 1em">
            Completeness of questionnaires
          </h3>
          <ProgressMeter
            :chartId="icsCompletionChart?.chartId"
            :title="icsCompletionChart?.chartTitle?.replace('${ageGroup}', (selectedCleftType as string))"
            :value="icsCompletionChartData?.dataPointValue"
            :totalValue="100"
            :barHeight="20"
            barFill="#9f6491"
          />
          <ProgressMeter
            :chartId="cleftqCompletionChart?.chartId"
            :title="cleftqCompletionChart?.chartTitle?.replace('${ageGroup}', (selectedCleftType as string))"
            :value="cleftqCompletionChartData?.dataPointValue"
            :totalValue="100"
            :barHeight="20"
            barFill="#66c2a4"
            style="margin-top: 1em"
          />
        </DashboardChart>
      </template>
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
import LoadingBlock from "../../components/LoadingBlock.vue";
import ProviderDashboard from "../../components/ProviderDashboard.vue";

import { generateAxisTickData } from "../../utils/generateAxisTicks";
import { asKeyValuePairs, sum, uniqueValues } from "../../utils";
import { generateColorPalette } from "../../utils/generateColorPalette";
import { getDashboardChart } from "../../utils/getDashboardData";

import type { ICharts, IChartData } from "../../types/schema";
import type { IAppPage } from "../../types/app";
import type { IKeyValuePair } from "../../types/index";
const props = defineProps<IAppPage>();

const loading = ref<boolean>(true);
const cleftTypeOptions = ref<string[]>();
const selectedCleftType = ref<string>();
const selectedCleftTypePatients = computed<number>(() => {
  if (selectedCleftType.value) {
    const newCleftType = patientsByPhenotypeChartData.value?.filter(
      (row: IChartData) => {
        return row.dataPointName === selectedCleftType.value;
      }
    ) as IChartData[];
    return newCleftType[0].dataPointValue as number;
  } else {
    return 0;
  }
});

const patientsByPhenotypeChart = ref<ICharts>();
const patientsByPhenotypeChartData = ref<IChartData[]>();
const patientsByPhenotypePalette = ref<IKeyValuePair>();
const patientsByGenderChart = ref<ICharts>();
const patientsByGenderChartData = ref<IKeyValuePair>();
const patientsByGenderPalette = ref<IKeyValuePair>();
const icsCompletionChart = ref<ICharts>();
const icsCompletionChartData = ref<IChartData>();
const cleftqCompletionChart = ref<ICharts>();
const cleftqCompletionChartData = ref<IChartData>();

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

function updateGenderChart() {
  const filteredData = patientsByGenderChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedCleftType.value;
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
  icsCompletionChartData.value = icsCompletionChart.value?.dataPoints?.filter(
    (row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedCleftType.value;
    }
  )[0] as IChartData;

  cleftqCompletionChartData.value =
    cleftqCompletionChart.value?.dataPoints?.filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === selectedCleftType.value;
    })[0] as IChartData;
}

function updateCharts() {
  updateGenderChart();
  updateProgressMeter();
}

onMounted(() => {
  getPageData()
    .then(() => {
      if (patientsByPhenotypeChart.value?.dataPoints) {
        patientsByPhenotypeChartData.value =
          patientsByPhenotypeChart.value.dataPoints;

        const chartTicks = generateAxisTickData(
          patientsByPhenotypeChartData.value,
          "dataPointValue"
        );

        patientsByPhenotypeChart.value.yAxisMaxValue = chartTicks.limit;
        patientsByPhenotypeChart.value.yAxisTicks = chartTicks.ticks;

        cleftTypeOptions.value = patientsByPhenotypeChart.value?.dataPoints.map(
          (row: IChartData) => row.dataPointName
        ) as string[];
        selectedCleftType.value = cleftTypeOptions.value[0];
      }

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
    .then(() => updateCharts())
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>
