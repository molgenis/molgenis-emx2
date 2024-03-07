<template>
  <p v-if="chartLoading">Chart is loading...</p>
  <p v-if="chartError">{{ chartError }}</p>
  <p v-if="chartSuccess">{{ chartData }}</p>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { gql, request } from "graphql-request";
import BarChart from "./BarChart.vue";
import type { BarChartParams } from "../../utils/types/viz";
import { useFetch, UseFetchState } from "../../utils/useFetch.js";

// const props = withDefaults(
//   defineProps<BarChartParams>(), {
//     chartId
//     chartHeight: 200,
//   }
// );

let chartLoading = ref<Boolean>(false);
let chartSuccess = ref<Boolean>(true);
let chartData = ref<Array[]>([]);
let chartError = ref<Error | null>(null);


onMounted(async () => {
  const { loading, success, data, error } = await useFetch(
    "../api/graphql",
    gql`query {
      ClinicalDta {
      n
    }
    }`
  )
  
  chartLoading.value = loading;
  chartSuccess.value = success;
  chartData.value = data;
})

</script>
