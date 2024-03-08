<template>
  {{ chartData }}
  <LoadingScreen v-if="chartLoading" message="" />
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <BarChart
    v-if="!chartLoading && !chartError && chartSuccess"
    chartId="myChart"
    title="Test"
    description="Some description"
    :chartData="chartData"
    xvar="_sum"
    yvar="researchCenter"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch } from "vue";
import { gql, request } from "graphql-request";
import BarChart from "./BarChart.vue";
import type { BarChartParams } from "../../utils/types/viz";
import { useFetch, UseFetchState } from "../../utils/useFetch.js";
import MessageBox from "../display/MessageBox.vue";
import LoadingScreen from "../display/LoadingScreen.vue";

// const props = withDefaults(defineProps<BarChartParams>(), {
//   chartHeight: 200,
//   xvar: "",
//   yvar: "",
//   chartData: [],
// });

defineProps<BarChartParams>();

let chartLoading = ref<Boolean>(false);
let chartSuccess = ref<Boolean>(true);
let chartError = ref<Error | null>(null);
let chartData = ref<Array[]>([]);
let rawData = ref<Array[]>([]);

function extractNestedRowData(
  row: object,
  key: string,
  nestedKey: string
): string | number {
  return typeof row[key] === "object" ? row[key][nestedKey] : row[key];
}

function prepRowData(
  data: Array[],
  x: string,
  y: string,
  nested_x_key: string,
  nested_y_key: string
) {
  return data.map((row: object) => {
    const newRow: object = {};
    newRow[x] = extractNestedRowData(row, x, nested_x_key);
    newRow[y] = extractNestedRowData(row, y, nested_y_key);
    return newRow;
  });
}

onBeforeMount(async () => {
  const { loading, success, data, error } = await useFetch(
    "../api/graphql",
    gql`
      query {
        ClinicalData_groupBy {
          researchCenter {
            name
          }
          _sum {
            n
          }
        }
      }
    `
  );
  
  rawData.value = data.value.ClinicalData_groupBy;
  chartData.value = prepRowData(
    rawData.value,
    "researchCenter",
    "_sum",
    "name",
    "n"
  );
  
  chartLoading.value = loading.value;
  chartSuccess.value = success.value;
  chartError.value = error.value;
});
</script>
