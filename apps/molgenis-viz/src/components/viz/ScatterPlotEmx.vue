<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <ScatterPlot
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :xvar="xvar"
    :yvar="yvar"
    :group="group"
    :xMin="xMin"
    :xMax="xMax"
    :yMin="yMin"
    :yMax="yMax"
    :xTickValues="xTickValues"
    :yTickValues="yTickValues"
    :xAxisLabel="xAxisLabel"
    :yAxisLabel="yAxisLabel"
    :pointRadius="pointRadius"
    :pointFill="pointFill"
    :pointFillPalette="pointFillPalette"
    :chartHeight="chartHeight"
    :chartMargins="chartMargins"
    :enableClicks="enableClicks"
    :enableTooltip="enableTooltip"
    :tooltipTemplate="tooltipTemplate"
    :enableChartLegend="enableChartLegend"
    :stackLegend="stackLegend"
    :enableLegendClicks="enableLegendClicks"
    @point-clicked="(data: object) => chartDataClicked = data"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";

import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz";

import type { ScatterPlotParams } from "../../interfaces/viz";
import ScatterPlot from "./ScatterPlot.vue";
import LoadingScreen from "../display/LoadingScreen.vue";
import MessageBox from "../display/MessageBox.vue";

const props = withDefaults(defineProps<ScatterPlotParams>(), {
  enableTooltip: true,
});

const emit = defineEmits<{
  (e: "viz-data-clicked", row: object): void;
}>();

let chartLoading = ref<Boolean>(true);
let chartSuccess = ref<Boolean>(false);
let chartError = ref<Error | null>(null);
let chartData = ref<Array[]>([]);
let chartDataQuery = ref<string | null>(null);
let chartDataClicked = ref<object | null>(null);

let xVar = ref<string | null>(null);
let yVar = ref<string | null>(null);
let groupVar = ref<string | null>(null);
let xSubSelection = ref<string | null>(null);
let ySubSelection = ref<string | null>(null);
let groupSubSelection = ref<string | null>(null);

function setChartVariables() {
  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);

  if (props.group) {
    groupVar.value = gqlExtractSelectionName(props.group);
    groupSubSelection.value = gqlExtractSubSelectionNames(props.group);
  }

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
    chartData.value = await prepareChartData({
      data: data,
      chartVariables: [
        { key: xVar.value, nestedKey: xSubSelection.value },
        { key: yVar.value, nestedKey: ySubSelection.value },
        { key: groupVar.value, nestedKey: groupSubSelection.value },
      ],
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

watch(chartDataClicked, () => {
  if (props.enableClicks) {
    emit("viz-data-clicked", chartDataClicked.value);
  }
});
</script>
