<template>
  <p>Test: {{ loading }}</p>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { gql } from "graphql-request";
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
    "/api/graphql", gql`ClinicalData { n }`
  )
  
  console.log(loading, success, data, error);
})

</script>
