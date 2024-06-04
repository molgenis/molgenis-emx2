<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <PieChart2
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :valuesAreShown="valuesAreShown"
    :valuesArePercents="valuesArePercents"
    :chartHeight="chartHeight"
    :chartMargins="chartMargins"
    :chartScale="chartScale"
    :chartColors="chartColors"
    :strokeColor="strokeColor"
    :asDonutChart="asDonutChart"
    :centerAlignChart="centerAlignChart"
    :enableHoverEvents="enableHoverEvents"
    :enableClicks="enableClicks"
    :enableChartLegend="enableChartLegend"
    :stackLegend="stackLegend"
    :legendPosition="legendPosition"
    :enableLegendClicks="enableLegendClicks"
    :enableLegendHovering="enableLegendHovering"
    @slice-clicked="onClick"
  />
</template>

<script lang="ts" setup>
import { ref, onBeforeMount, watch, computed } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";

import { setGraphQlEndpoint } from "../../utils";
import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz";

import type { PieChartParams } from "../../interfaces/viz";
import PieChart2 from "./PieChart2.vue";
import LoadingScreen from "../display/LoadingScreen.vue";
import MessageBox from "../display/MessageBox.vue";

const props = withDefaults(defineProps<PieChartParams>(), {
  enableChartLegend: true,
  enableLegendHovering: true,
  chartScale: 1,
  asDonutChart: true,
});

const emit = defineEmits<{
  (e: "viz-data-clicked", row: object): void;
}>();

const graphqlEndpoint = ref<string | null>(null);

const chartLoading = ref<boolean>(true);
const chartSuccess = ref<boolean>(false);
const chartError = ref<Error | null>(null);
const chartDataQuery = ref<string | null>(null);

const chartData = ref<object>([]);
const categoriesVar = ref<string | null>(null);
const valuesVar = ref<string | null>(null);
const categoriesSubSelection = ref<string | null>(null);
const valuesSubSelection = ref<string | null>(null);

function setChartVariables() {
  graphqlEndpoint.value = setGraphQlEndpoint(props.schema);

  categoriesVar.value = gqlExtractSelectionName(props.categories);
  valuesVar.value = gqlExtractSelectionName(props.values);
  categoriesSubSelection.value = gqlExtractSubSelectionNames(props.categories);
  valuesSubSelection.value = gqlExtractSubSelectionNames(props.values);
  chartDataQuery.value = buildQuery({
    table: props.table,
    selections: [props.categories, props.values],
  });
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  chartError.value = null;

  try {
    const response = await request(graphqlEndpoint.value, chartDataQuery.value);
    const data = await response[props.table as string];
    const preppedData = await prepareChartData({
      data: data,
      chartVariables: [
        { key: categoriesVar.value, nestedKey: categoriesSubSelection.value },
        { key: valuesVar.value, nestedKey: valuesSubSelection.value },
      ],
    });

    const entries = preppedData
      .sort(
        (currRow, nextRow) =>
          nextRow[valuesVar.value] - currRow[valuesVar.value]
      )
      .map((row) => [row[categoriesVar.value], row[valuesVar.value]]);
    chartData.value = Object.fromEntries(entries);

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

watch(
  [chartDataQuery, categoriesSubSelection, valuesSubSelection],
  async () => {
    await fetchChartData();
  }
);
</script>
