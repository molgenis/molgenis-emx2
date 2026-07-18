<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  PieChart2,
  InputLabel,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";

import ProviderDashboard from "../../components/ProviderDashboard.vue";
import { generateAxisTickData } from "../../../../tailwind-components/app/utils/viz";
import { asKeyValuePairs, uniqueValues } from "../../utils";
import { generateColorPalette } from "../../utils/generateColorPalette";
import { getDashboardChart } from "../../../../metadata-utils/src/viz/getUiDashboardCharts";

import type {
  ICharts,
  IChartData,
} from "../../../../metadata-utils/src/viz/UiDashboard";
import type { IAppPage, IKeyValuePair } from "../../types";

const props = defineProps<IAppPage>();
const loading = ref<boolean>(true);
const cleftTypeOptions = ref<string[]>();
const selectedCleftType = ref<string>("");
const selectedCleftTypePatients = computed<number>(() => {
  if (selectedCleftType.value) {
    const newCleftType = patientsByPhenotypeChartData.value?.filter(
      (row: IChartData) => {
        return row.name === selectedCleftType.value;
      }
    ) as IChartData[];
    return newCleftType[0].value as number;
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

async function getPageData() {
  const phenotypesResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-patients-by-phenotype"
  );

  const genderResponse = await getDashboardChart(
    props.api.graphql.current,
    "clp-provider-patients-by-gender"
  );

  patientsByPhenotypeChart.value = phenotypesResponse[0];
  patientsByGenderChart.value = genderResponse[0];
}

function updateGenderChart() {
  const filteredData = patientsByGenderChart.value?.dataPoints
    ?.filter((row: IChartData) => {
      return row.primaryCategory === selectedCleftType.value;
    })
    .sort((a: IChartData, b: IChartData): number => {
      return (b.value as number) - (a.value as number);
    });

  patientsByGenderChartData.value = asKeyValuePairs(
    filteredData,
    "name",
    "value"
  );
}

function updateCharts() {
  updateGenderChart();
}

onMounted(() => {
  getPageData()
    .then(() => {
      if (patientsByPhenotypeChart.value?.dataPoints) {
        patientsByPhenotypeChartData.value =
          patientsByPhenotypeChart.value.dataPoints;

        const chartTicks = generateAxisTickData(
          patientsByPhenotypeChartData.value,
          "value"
        );

        patientsByPhenotypeChart.value.yAxisMaxValue = chartTicks.limit;
        patientsByPhenotypeChart.value.yAxisTicks = chartTicks.ticks;

        cleftTypeOptions.value = patientsByPhenotypeChart.value?.dataPoints.map(
          (row: IChartData) => row.name
        ) as string[];
        selectedCleftType.value = cleftTypeOptions.value[0];
      }

      const phenotypeCategories = uniqueValues(
        patientsByPhenotypeChart.value?.dataPoints,
        "name"
      );

      patientsByPhenotypePalette.value =
        generateColorPalette(phenotypeCategories);

      const genderCategories = uniqueValues(
        patientsByGenderChart.value?.dataPoints,
        "name"
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
          xvar="name"
          yvar="value"
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
    <h2 class="dashboard-h2">
      Overview of {{ selectedCleftType }} patients (n={{
        selectedCleftTypePatients
      }})
    </h2>
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
  </ProviderDashboard>
</template>
