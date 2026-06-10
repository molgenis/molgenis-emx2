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
import type { IAxisTickData } from "../types";

const props = withDefaults(
  defineProps<{
    chart: ICharts;
    enableFilter?: boolean;
    filterProperty?: string;
    filterTitle?: string;
    ernLevelData?: IChartData[];
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
      return (
        a.dataPointName?.localeCompare(b.dataPointName as string) ||
        (a.dataPointOrder as number) - (b.dataPointOrder as number)
      );
    });
  }

  return data;
});

const chartDescription = computed<string>(() => {
  let description = "Number of patients: ";

  if (props.chart.dataPoints) {
    const centerSum = props.chart.dataPoints?.reduce(
      (acc: number, row: IChartData) => {
        acc += row.dataPointValue as number;
        return acc;
      },
      0
    );

    description += `Center (n=${centerSum}) `;
  }

  if (props.ernLevelData) {
    const ernSum = props.ernLevelData?.reduce(
      (acc: number, row: IChartData) => {
        acc += row.dataPointValue as number;
        return acc;
      },
      0
    );
    description += `ERN (n=${ernSum})`;
  }

  return description;
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
      columnHoverFill="#708fb4"
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
