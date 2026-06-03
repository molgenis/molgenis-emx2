<script lang="ts" setup>
import { ref } from "vue";

// @ts-expect-error
import {
  LoadingScreen,
  GroupedColumnChart,
  DashboardChart,
} from "molgenis-viz";
import { generateAxisTickData } from "../utils/generateAxisTicks";

import type { ICharts, IChartData } from "../types/schema";
import type { IAxisTickData } from "../types";

const props = defineProps<{ chart: ICharts }>();
const loading = ref<boolean>(true);

const colorPalette = {
  ERN: "#B98DAF", // "#9f6491",
  "Your center": "#A7DCCB", //"#66c2a4",
};

// TODO: add data from "providers" schema and add secondary categorical variable
const chartData = ref<IChartData[]>(props.chart.dataPoints as IChartData[]);
const chartTicks = ref<IAxisTickData>();

chartTicks.value = generateAxisTickData(chartData, "dataPointValue");

if (props.chart.dataPoints) {
  loading.value = false;
}
</script>

<template>
  <DashboardChart :id="chart.chartId">
    <LoadingScreen v-if="loading" style="height: 250px" />
    <GroupedColumnChart
      v-else
      :chartId="chart.chartId"
      :title="chart.chartTitle"
      :description="chart.chartSubtitle"
      :chartData="chartData"
      xvar="dataPointName"
      yvar="dataPointValue"
      group="dataPointSecondaryCategory"
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
