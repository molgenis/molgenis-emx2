<script lang="ts" setup>
import { ref, computed } from "vue";

import {
  LoadingScreen,
  GroupedColumnChart,
  DashboardChart,
  MessageBox,
  // @ts-expect-error
} from "molgenis-viz";
import { generateAxisTickData } from "../utils/generateAxisTicks";
import { ernYourCenterPalette } from "../utils/variables";

import type { ICharts, IChartData } from "../types/schema";
import type {
  IAxisTickData,
  ICleftTypes,
  ISiteErnCleftTypeCounts,
} from "../types";

const props = withDefaults(
  defineProps<{
    chart: ICharts;
    enableFilter?: boolean;
    filterProperty?: string;
    filterTitle?: string;
    ernLevelData?: IChartData[];
    numberOfPatientsByCleftType?: ISiteErnCleftTypeCounts;
    chartDescription?: string;
  }>(),
  {
    enableFilter: false,
    filterTitle: "Filter chart",
  }
);
const loading = ref<boolean>(true);
const error = ref<string>();

const chartData = computed<IChartData[]>(() => {
  let data = props.chart.dataPoints as IChartData[];

  if (props.ernLevelData) {
    const ernData = props.ernLevelData.map((row) => {
      row.dataPointSecondaryCategory = "ERN";
      return row;
    });
    data = data.concat(ernData as IChartData[]);
    data = data.sort((a: IChartData, b: IChartData) => {
      return (a.dataPointOrder as number) - (b.dataPointOrder as number);
    });
  }

  return data;
});

const chartDescription = computed<string>(() => {
  if (props.enableFilter && props.numberOfPatientsByCleftType) {
    const centerValue =
      props.numberOfPatientsByCleftType.center[
        selectedFilter.value as keyof ICleftTypes
      ];
    const ernValue =
      props.numberOfPatientsByCleftType.ern[
        selectedFilter.value as keyof ICleftTypes
      ];
    return `${selectedFilter.value} patients from your center (n=${centerValue}) and the ERN (n=${ernValue})`;
  } else if (props.chartDescription) {
    return props.chartDescription;
  } else {
    return "";
  }
});

const chartTicks = ref<IAxisTickData>();
const chartFilters = ref<string[]>();
const selectedFilter = ref<string>();

const filteredData = computed<IChartData[]>(() => {
  return (chartData.value as IChartData[]).filter(
    (row) =>
      row[props.filterProperty as keyof IChartData] === selectedFilter.value
  );
});

chartTicks.value = generateAxisTickData(chartData.value, "dataPointValue");

if (props.enableFilter) {
  chartFilters.value = [
    ...new Set(
      props.chart.dataPoints?.map((row: IChartData) => {
        return row[props.filterProperty as keyof IChartData];
      })
    ),
  ] as string[];

  selectedFilter.value = chartFilters.value[0];
}

if (props.chart.dataPoints) {
  loading.value = false;
} else {
  error.value = `No data is associated with chart ${props.chart.chartTitle}`;
}
</script>

<template>
  <DashboardChart :id="chart.chartId">
    <LoadingScreen v-if="loading" />
    <MessageBox v-else-if="error" type="error">
      {{ error }}
    </MessageBox>
    <GroupedColumnChart
      v-else
      :chartId="chart.chartId"
      :title="chart.chartTitle"
      :description="chartDescription"
      :chartData="filteredData"
      xvar="dataPointSecondaryCategory"
      yvar="dataPointValue"
      group="dataPointName"
      :xAxisLabel="chart.xAxisLabel"
      :yAxisLabel="chart.yAxisLabel"
      :yMin="0"
      :yMax="chartTicks?.limit"
      :yTickValues="chartTicks?.ticks"
      :columnColorPalette="ernYourCenterPalette"
      columnHoverFill="#EE7032"
      :chartHeight="250"
      :chartMargins="{
        top: chart.topMargin,
        right: chart.rightMargin,
        bottom: chart.bottomMargin,
        left: chart.leftMargin,
      }"
    >
    </GroupedColumnChart>
    <form v-if="enableFilter && chartFilters" @submit.prevent class="mt-2">
      <label :form="`${chart.chartId}-filter`" class="">
        {{ filterTitle }}
      </label>
      <select
        :id="`${chart.chartId}-filter`"
        v-model="selectedFilter"
        class="custom-select"
      >
        <option v-for="option in chartFilters" :value="option" :key="option">
          {{ option }}
        </option>
      </select>
    </form>
  </DashboardChart>
</template>
