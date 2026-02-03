<script lang="ts" setup>
import PieChart from "../../components/viz/PieChart/PieChart.vue";

const chartData = {
  "Group A": 0.5,
  "Group B": 0.2,
  "Group C": 0.3,
};
</script>
<template>
  <PieChart
    id="pie-chart-demo"
    title="Group assignment"
    :chart-data="chartData"
    :as-donut-chart="true"
  />
</template>
