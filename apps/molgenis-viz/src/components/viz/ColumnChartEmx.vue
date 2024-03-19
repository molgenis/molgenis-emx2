<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <ColumnChart
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :xvar="xVar"
    :yvar="yVar"
    :yMax="yMax"
    :yTickValues="yTickValues"
    :xAxisLabel="xAxisLabel"
    :yAxisLabel="yAxisLabel"
    :xAxisLineBreaker="xAxisLineBreaker"
    :chartHeight="chartHeight"
    :chartMargins="chartMargins"
    :columnFill="columnFill"
    :columnHoverFill="columnHoverFill"
    :columnColorPalette="columnColorPalette"
    :columnPaddingInner="columnPaddingInner"
    :columnPaddingOuter="columnPaddingOuter"
    :columnAlign="columnAlign"
    :enableClicks="enableClicks"
    :enableAnimation="enableAnimation"
  />
  <!-- <div>
      <code>x: {{ xVar }}</code><br />
      <code>nested: {{ xSubSelection }}</code>
    </div>
    <div>
      <code>y: {{ yVar }}</code><br />
      <code>nested: {{ ySubSelection }}</code>
    </div> 
    <pre>{{ chartData }}</pre> -->
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";
import type { ColumnChartParams } from "../../interfaces/viz.ts";
import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz.ts";

import ColumnChart from "./ColumnChart.vue";
import MessageBox from "../display/MessageBox.vue";
import LoadingScreen from "../display/LoadingScreen.vue";

const props = defineProps<ColumnChartParams>();

let chartLoading = ref<Boolean>(true);
let chartSuccess = ref<Boolean>(false);
let chartError = ref<Error | null>(null);
let chartData = ref<Array[]>([]);
let chartDataQuery = ref<string | null>(null);
let xVar = ref<string | null>(null);
let yVar = ref<string | null>(null);
let xSubSelection = ref<string | null>(null);
let ySubSelection = ref<string | null>(null);

function setQuerySelections() {
  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  try {
    const response = await request("../api/graphql", chartDataQuery.value);
    const data = await response[props.table as string];
    chartData.value = await prepareChartData(
      data,
      xVar.value,
      yVar.value,
      xSubSelection.value,
      ySubSelection.value
    );
    chartSuccess.value = true;
  } catch (error) {
    chartError.value = error;
  } finally {
    chartLoading.value = false;
  }
}

onBeforeMount(() => {
  chartDataQuery.value = buildQuery(props.table, props.xvar, props.yvar);
  setQuerySelections();
});

watch(props, () => {
  chartDataQuery.value = buildQuery(props.table, props.xvar, props.yvar);
  setQuerySelections();
});

watch([chartDataQuery, xSubSelection, ySubSelection], async () => {
  await fetchChartData();
});
</script>
