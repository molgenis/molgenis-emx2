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
    @column-clicked="onClick"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch, computed } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";
import type { ColumnChartParams } from "../../interfaces/viz.ts";

import { setGraphQlEndpoint } from "../../utils";
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
const emit = defineEmits<{
  (e: "viz-data-clicked", row: object): void;
}>();

const graphqlEndpoint = ref<string | null>(null);

const chartLoading = ref<boolean>(true);
const chartSuccess = ref<boolean>(false);
const chartError = ref<Error | null>(null);
const chartData = ref<object[]>([]);
const chartDataQuery = ref<string | null>(null);

const xVar = ref<string | null>(null);
const yVar = ref<string | null>(null);
const xSubSelection = ref<string | null>(null);
const ySubSelection = ref<string | null>(null);

function setChartVariables() {
  graphqlEndpoint.value = setGraphQlEndpoint(props.schema);

  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);
  chartDataQuery.value = buildQuery({
    table: props.table,
    selections: [props.xvar, props.yvar],
  });
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  chartError.value = null;

  try {
    const response = await request(graphqlEndpoint.value, chartDataQuery.value);
    const data = await response[props.table as string];
    chartData.value = await prepareChartData({
      data: data,
      chartVariables: [
        { key: xVar.value, nestedKey: xSubSelection.value },
        { key: yVar.value, nestedKey: ySubSelection.value },
      ],
    });
    chartSuccess.value = true;
  } catch (error) {
    chartError.value = error;
  } finally {
    chartLoading.value = false;
  }
}

function onClick(data: object): object | null {
  if (props.enableClicks) {
    emit("viz-data-clicked", data);
  }
}

onBeforeMount(() => setChartVariables());
watch(props, () => setChartVariables());

watch([chartDataQuery, xSubSelection, ySubSelection], async () => {
  await fetchChartData();
});
</script>
