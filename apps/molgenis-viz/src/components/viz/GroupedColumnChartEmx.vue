<template>
  <code>{{ props }}</code>
  <ul>
    <li><code>x: {{ xVar }}</code></li>
    <li><code>y: {{ yVar }}</code></li>
    <li><code>group: {{ groupVar }}</code></li>
  </ul>
  <code>{{ chartDataQuery }}</code>
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

const props = defineProps<GroupedColumnChartParams>();

let chartLoading = ref<Boolean>(false);
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

function setQuerySelections() {
  xVar.value = gqlExtractSelectionName(props.xvar);
  yVar.value = gqlExtractSelectionName(props.yvar);
  groupVar.value = gqlExtractSelectionName(props.group);
  xSubSelection.value = gqlExtractSubSelectionNames(props.xvar);
  ySubSelection.value = gqlExtractSubSelectionNames(props.yvar);
  groupSubSelection.value = gqlExtractSubSelectionNames(props.group);
}

onBeforeMount(() => {
  chartDataQuery.value = buildQuery(props.table, props.xvar, props.yvar)
  setQuery();
})

</script>
