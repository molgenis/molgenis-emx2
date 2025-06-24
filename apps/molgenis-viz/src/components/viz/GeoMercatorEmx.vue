<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <GeoMercator
    v-if="chartSuccess"
    :chartId="chartId"
    :title="title"
    :description="description"
    :chartData="chartData"
    :geojson="WorldGeoJson"
    :rowId="rowId"
    :latitude="latVar"
    :longitude="lngVar"
    :group="groupVar"
    :groupColorMappings="groupColorMappings"
    :markerColor="markerColor"
    :markerStroke="markerStroke"
    :chartHeight="chartHeight"
    :mapCenter="mapCenter"
    :chartSize="chartSize"
    :chartScale="chartScale"
    :pointRadius="pointRadius"
    :legendData="groupColorMappings"
    :showTooltip="showTooltip"
    :tooltipTemplate="tooltipTemplate"
    :enableMarkerClicks="enableMarkerClicks"
    :enableLegendClicks="enableLegendClicks"
    :enableZoom="enableZoom"
    :zoomLimits="zoomLimits"
    :mapColors="mapColors"
    @marker-clicked="onClick"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch, computed } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";

import { setGraphQlEndpoint } from "../../utils";
import type {
  GeoMercatorParams,
  gqlVariableSubSelectionIF,
} from "../../interfaces/viz";

import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz";

import WorldGeoJson from "../../data/world.geo.json";
import GeoMercator from "./GeoMercator.vue";
import LoadingScreen from "../display/LoadingScreen.vue";
import MessageBox from "../display/MessageBox.vue";

interface GeoMercatorEmxParams extends GeoMercatorParams {
  geojson?: object;
  chartData?: object[];
  legendData?: object;
}

const props = withDefaults(defineProps<GeoMercatorEmxParams>(), {
  showTooltip: true,
  enableZoom: true,
  enableLegendClicks: true,
});

const emit = defineEmits<{
  (e: "viz-data-clicked", row: object): void;
}>();

const graphqlEndpoint = ref<string | null>(null);

const chartLoading = ref<boolean>(true);
const chartSuccess = ref<boolean>(false);
const chartError = ref<Error | null>(null);
const chartData = ref<object[]>([]);
const chartDataQuery = ref<string | null>(null);

const rowId = ref<string | null>(null);
const latVar = ref<string | null>(null);
const lngVar = ref<string | null>(null);
const groupVar = ref<string | null>(null);
const rowIdSubSelection = ref<string | null>(null);
const latSubSelection = ref<string | null>(null);
const lngSubSelection = ref<string | null>(null);
const groupSubSelection = ref<string | null>(null);
const legendData = ref<object | null>(null);

let tooltipVars = ref<gqlVariableSubSelectionIF[] | null>(null);

function setChartVariables() {
  graphqlEndpoint.value = setGraphQlEndpoint(props.schema);

  rowId.value = gqlExtractSelectionName(props.rowId);
  latVar.value = gqlExtractSelectionName(props.latitude);
  lngVar.value = gqlExtractSelectionName(props.longitude);
  rowIdSubSelection.value = gqlExtractSubSelectionNames(props.rowId);
  latSubSelection.value = gqlExtractSubSelectionNames(props.latitude);
  lngSubSelection.value = gqlExtractSubSelectionNames(props.longitude);

  const chartSelections = [
    props.latitude,
    props.longitude,
    props.rowId,
    props.group,
  ];

  if (props.group) {
    groupVar.value = gqlExtractSelectionName(props.group);
    groupSubSelection.value = gqlExtractSubSelectionNames(props.group);
  }

  if (props.tooltipVariables) {
    chartSelections.push(...props.tooltipVariables);
    tooltipVars.value = props.tooltipVariables.map((variable: string) => {
      return {
        key: gqlExtractSelectionName(variable),
        nestedKey: gqlExtractSubSelectionNames(variable),
      };
    });
  }

  chartDataQuery.value = buildQuery({
    table: props.table,
    selections: chartSelections,
  });
}

async function fetchChartData() {
  chartLoading.value = true;
  chartSuccess.value = false;
  chartError.value = null;

  try {
    const response = await request(graphqlEndpoint.value, chartDataQuery.value);
    const data = await response[props.table as string];

    const chartColumns = [
      { key: rowId.value, nestedKey: rowIdSubSelection.value },
      { key: latVar.value, nestedKey: latSubSelection.value },
      { key: lngVar.value, nestedKey: lngSubSelection.value },
      { key: groupVar.value, nestedKey: groupSubSelection.value },
    ];

    if (tooltipVars.value?.length) {
      chartColumns.push(...tooltipVars.value);
    }

    chartData.value = await prepareChartData({
      data: data,
      chartVariables: chartColumns,
    });
    chartSuccess.value = true;
  } catch (error) {
    chartError.value = error;
  } finally {
    chartLoading.value = false;
  }
}

function onClick(data: object): object | null {
  if (props.enableMarkerClicks) {
    emit("viz-data-clicked", data);
  }
}

onBeforeMount(() => setChartVariables());
watch(props, () => setChartVariables());

watch(chartDataQuery, async () => {
  await fetchChartData();
});
</script>
