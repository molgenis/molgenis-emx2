<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <BarChart
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :xvar="xVar"
    :yvar="yVar"
    :xMax="xMax"
    :xTickValues="xTickValues"
    :xAxisLabel="xAxisLabel"
    :yAxisLabel="yAxisLabel"
    :yAxisLineBreaker="yAxisLineBreaker"
    :chartHeight="chartHeight"
    :chartMargins="chartMargins"
    :barFill="barFill"
    :barHoverFill="barHoverFill"
    :barColorPalette="barColorPalette"
    :barPaddingInner="barPaddingInner"
    :barPaddingOuter="barPaddingOuter"
    :barAlign="barAlign"
    :enableClicks="enableClicks"
    :enableAnimation="enableAnimation"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";
import type { BarChartParams } from "../../interfaces/viz.ts";
import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz.ts";

import BarChart from "./BarChart.vue";
import MessageBox from "../display/MessageBox.vue";
import LoadingScreen from "../display/LoadingScreen.vue";

const props = defineProps<BarChartParams>();

let chartLoading = ref<Boolean>(true);
let chartSuccess = ref<Boolean>(false);
let chartError = ref<Error | null>(null);
let chartData = ref<Array[]>([]);
let chartDataQuery = ref<string | null>(null);
let xVar = ref<string | null>(null);
let yVar = ref<string | null>(null);
let xSubSelection = ref<string | null>(null);
let ySubSelection = ref<string | null>(null);

function setChartVariables() {
  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);
  chartDataQuery.value = buildQuery(props.table, props.xvar, props.yvar);
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  try {
    const response = await request("../api/graphql", chartDataQuery.value);
    const data = await response[props.table as string];
    chartData.value = await prepareChartData({
      data: data,
      x: xVar.value,
      y: yVar.value,
      nestedXKey: xSubSelection.value,
      nestedYKey: ySubSelection.value,
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
