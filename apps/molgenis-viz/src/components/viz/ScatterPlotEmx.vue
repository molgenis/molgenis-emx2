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
    :yxAxisLabel="yAxisLabel"
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

const props = withDefaults(defineProps<>(), {
  
});

let chartLoading = ref<Boolean>(true);
let chartError = ref<Error | null>(null);
let chartSuccess = ref<Boolean>(false);
</script>