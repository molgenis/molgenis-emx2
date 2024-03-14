<template>
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <div v-if="chartSuccess"> 
    <BarChart
      chartId="myChart"
      :title="title"
      :description="description"
      :chartData="chartData"
      :xvar="xVar"
      :yvar="yVar"
      :xMax="xMax"
      :xTickValues="xTickValues"
      :xAxisLabel="xAxisLabel"
      :yAxisLabel="yAxisLabel"
      yAxisLineBreaker=" "
      :chartMargins="chartMargins"
    />
    <div>
      <code>x: {{ xVar }}</code><br />
      <code>nested: {{ xSubSelection }}</code>
    </div>
    <div>
      <code>y: {{ yVar }}</code><br />
      <code>nested: {{ ySubSelection }}</code>
    </div> 
    <pre>{{ chartData }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeMount, onMounted, watch } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";
import { parse } from "graphql";
import BarChart from "./BarChart.vue";
import type { BarChartParams } from "../../utils/types/viz";
import { useFetch, UseFetchState } from "../../utils/useFetch.js";
import MessageBox from "../display/MessageBox.vue";
import LoadingScreen from "../display/LoadingScreen.vue";

const props = withDefaults(defineProps<BarChartParams>(), {
  chartHeight: 150,
});

let chartLoading = ref<Boolean>(true)
let chartSuccess = ref<Boolean>(false);
let chartError = ref<Error | null>(null);
let chartData = ref<Array[]>([]);
let chartDataQuery = ref<string | null>(null);
let xVar = ref<string | null>(null);
let yVar = ref<string | null>(null);
let xSubSelection = ref<string | null>(null);
let ySubSelection = ref<string | null>(null);

function extractNestedRowData(
  row: object,
  key: string,
  nestedKey: string
): string | number {
  return typeof row[key] === "object" ? row[key][nestedKey] : row[key];
}

function prepRowData(data: Array[]) {
  return data.map((row: object) => {
    const newRow: object = {};
    newRow[xVar.value] = extractNestedRowData(row, xVar.value, xSubSelection.value);
    newRow[yVar.value] = extractNestedRowData(row, yVar.value, ySubSelection.value);
    return newRow;
  });  
}


function buildQuery(table, xvar, yvar) {
  chartDataQuery.value = `{
    ${props.table} {
      ${props.yvar}
      ${props.xvar}
    }
  }`
}

function getSelectionName (variable: string) {
  if (variable.match(/[\{\}]/)) {
    const query = gql`query { ${variable} }`;
    const selectionName = query.definitions[0].selectionSet?.selections[0].name?.value;
    if (selectionName) {
      return selectionName;
    }
  }
  return variable;
}

function getSubSelectionNames (variable: string) {
  if (variable.match(/[\{\}]/)) {
    const query = gql`query { ${variable} }`
    const selectionSet = query.definitions[0].selectionSet?.selections[0].selectionSet?.selections;
    if (selectionSet.length > 0) {
      const subSelections = selectionSet[0].name?.value;
      return subSelections;
    }
  }
}

function setQuerySelections () {
  xVar.value = getSelectionName(props.xvar);
  yVar.value = getSelectionName(props.yvar);
  xSubSelection.value = getSubSelectionNames(props.xvar);
  ySubSelection.value = getSubSelectionNames(props.yvar);
}

async function fetchChartData () {
  chartLoading.value = true;
  try {
    const response = await request("../api/graphql", chartDataQuery.value);
    const data = await response[props.table as string];
    chartData.value = await prepRowData(data)
    chartSuccess.value = true;
  } catch (error) {
    chartError.value = error;
  } finally {
    chartLoading.value = false;
  }
}

onBeforeMount(() => {
  buildQuery();
  setQuerySelections();  
})

onMounted(async () => {
  await fetchChartData()
})

watch(props, async () => {
  buildQuery();
  setQuerySelections();
  await fetchChartData();
})


</script>
