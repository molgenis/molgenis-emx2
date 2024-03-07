<template>
  {{ chartData }}
  <LoadingScreen v-if="chartLoading" message=""/>
  <MessageBox v-if="chartError" type="error">
    <output>{{ chartError }}</output>
  </MessageBox>
  <BarChart
    v-else
    chartId="myChart"
    title="Test"
    description="Some description"
    :chartData="chartData"
    xvar="n"
    yvar="researchCenter"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import { gql, request } from "graphql-request";
import BarChart from "./BarChart.vue";
import type { BarChartParams } from "../../utils/types/viz";
import { useFetch, UseFetchState } from "../../utils/useFetch.js";
import MessageBox from "../display/MessageBox.vue";
import LoadingScreen from "../display/LoadingScreen.vue";

const props = withDefaults(
  defineProps<BarChartParams>(), {
    chartId: null,
    chartHeight: 200,
    xvar: "",
    yvar: "",
  }
);

let chartLoading = ref<Boolean>(false);
let chartSuccess = ref<Boolean>(true);
let chartData = ref<Array[]>([]);
let chartError = ref<Error | null>(null);


onBeforeMount(async () => {
  const { loading, success, data, error } = await useFetch(
    "../api/graphql",
    gql`query {
      ClinicalData_groupBy {
        researchCenter {
          name
        }
        _sum {
          n
        }
      }
    }`
  )

  chartLoading.value = loading.value;
  chartSuccess.value = success.value;
  chartError.value = error.value;
  chartData.value = data.value;
})

</script>
