<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <GroupedColumnChart
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :xvar="xVar"
    :yvar="yVar"
    :group="groupVar"
    :yMax="yMax"
    :yTickValues="yTickValues"
    :xAxisLabel="xAxisLabel"
    :yAxisLabel="yAxisLabel"
    :xAxisLineBreaker="xAxisLineBreaker"
    :chartHeight="chartHeight"
    :columnColorPalette="columnColorPalette"
    :columnHoverFill="columnHoverFill"
    :columnPaddingInner="columnPaddingInner"
    :columnPaddingOuter="columnPaddingOuter"
    :columnAlign="columnAlign"
    :enableClicks="enableClicks"
    :enableAnimation="enableAnimation"
    :enableChartLegend="enableChartLegend"
    :stackLegend="stackLegend"
    :enableLegendClicks="enableLegendClicks"
  />
</template>

<script lang="ts" setup>
import { ref, onBeforeMount, watch } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";

import type { GroupedColumnChartParams } from "../../interfaces/viz";
import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz";

import GroupedColumnChart from "./GroupedColumnChart.vue";
import LoadingScreen from "../display/LoadingScreen.vue";
import MessageBox from "../display/MessageBox.vue";

const props = withDefaults(defineProps<GroupedColumnChartParams>(), {
  enableChartLegend: true,
  enableLegendClicks: true,
  enableAnimation: true,
});

let chartLoading = ref<Boolean>(true);
let chartError = ref<Error | null>(null);
let chartSuccess = ref<Boolean>(false);

let chartData = ref<Array[]>([]);
let chartDataQuery = ref<string | null>(null);
let xVar = ref<string | null>(null);
let yVar = ref<string | null>(null);
let groupVar = ref<string | null>(null);
let xSubSelection = ref<string | null>(null);
let ySubSelection = ref<string | null>(null);
let groupSubSelection = ref<string | null>(null);

function setChartVariables() {
  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  groupVar.value = gqlExtractSelectionName(props.group);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);
  groupSubSelection.value = gqlExtractSubSelectionNames(props.group);
  chartDataQuery.value = buildQuery({
    table: props.table,
    selections: [props.xvar, props.yvar, props.group],
  });
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  try {
    const response = await request("../api/graphql", chartDataQuery.value);
    const data = await response[props.table as string];
    const preppedData = await prepareChartData({
      data: data,
      chartVariables: [
        { key: xVar.value, nestedKey: xSubSelection.value },
        { key: yVar.value, nestedKey: ySubSelection.value },
        { key: groupVar.value, nestedKey: groupSubSelection.value },
      ],
    });

    chartData.value = preppedData.sort((current, next) => {
      return (
        current[groupVar.value].localeCompare(next[groupVar.value]) ||
        current[xVar.value].localeCompare(next[xVar.value])
      );
    });
    chartSuccess.value = true;
  } catch (error) {
    chartError.value = error;
  } finally {
    chartLoading.value = false;
  }
}

onBeforeMount(() => setChartVariables());

watch(props, () => setChartVariables());

watch([chartDataQuery, xSubSelection, ySubSelection], async () => {
  await fetchChartData();
});
</script>
