<script lang="ts" setup>
import { ref } from "vue";

import {
  LoadingScreen,
  GroupedColumnChart,
  DashboardChart,
  // @ts-expect-error
} from "molgenis-viz";
import { generateAxisTickData } from "../utils/generateAxisTicks";

import type { ICharts, IChartData } from "../types/schema";
import type { IAxisTickData } from "../types";

const props = withDefaults(
  defineProps<{
    chart: ICharts;
    enableFilter?: boolean;
    filterProperty?: string;
  }>(),
  {
    enableFilter: false,
  }
);
const loading = ref<boolean>(true);

const colorPalette = {
  ERN: "#B98DAF", // "#9f6491",
  "Your center": "#A7DCCB", //"#66c2a4",
};

// TODO: add data from "providers" schema and add secondary categorical variable
const chartData = ref<IChartData[]>(props.chart.dataPoints as IChartData[]);
const chartTicks = ref<IAxisTickData>();
const chartFilters = ref<string[]>();

chartTicks.value = generateAxisTickData(chartData.value, "dataPointValue");

if (props.chart.dataPoints) {
  loading.value = false;
}

if (props.enableFilter) {
  chartFilters.value = [
    ...new Set(
      props.chart.dataPoints?.map((row: IChartData) => {
        return row[props.filterProperty as keyof IChartData];
      })
    ),
  ] as string[];
}

function updateChart() {}
</script>

<template>
  {{ chartFilters }}
  <DashboardChart :id="chart.chartId">
    <LoadingScreen v-if="loading" style="height: 250px" />
    <GroupedColumnChart
      v-else
      :chartId="chart.chartId"
      :title="chart.chartTitle"
      :description="chart.chartSubtitle"
      :chartData="chartData"
      xvar="dataPointSecondaryCategory"
      yvar="dataPointValue"
      group="dataPointName"
      :xAxisLabel="chart.xAxisLabel"
      :yAxisLabel="chart.yAxisLabel"
      :yMin="0"
      :yMax="chartTicks?.limit"
      :yTickValues="chartTicks?.ticks"
      :columnColorPalette="colorPalette"
      columnHoverFill="#708fb4"
      :chartHeight="250"
      :chartMargins="{
        top: chart.topMargin,
        right: chart.rightMargin,
        bottom: chart.bottomMargin,
        left: chart.leftMargin,
      }"
    />
  </DashboardChart>
</template>
