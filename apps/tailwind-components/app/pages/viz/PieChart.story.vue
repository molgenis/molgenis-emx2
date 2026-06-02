<script lang="ts" setup>
import { ref } from "vue";

import PieChart from "../../components/viz/PieChart/PieChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";

const data = {
  "Group A": 48,
  "Group B": 32,
  "Group C": 11,
  Other: 9,
};

const clickedSlice = ref<Record<string, number>>();
</script>
<template>
  <div class="h-auto">
    <PieChart
      id="pie-chart-demo"
      title="Recruitment overview"
      description="Group assignment after randomization (n=100)"
      :data="data"
      :as-donut-chart="true"
      :click-events-are-enabled="true"
      :hover-events-are-enabled="true"
      :legend-is-enabled="true"
      legend-position="bottom"
      :show-values="true"
      :show-labels="true"
      @slice-clicked="clickedSlice = $event"
    />
    <ComponentOutput class="mt-2.5">
      Clicked segment: {{ clickedSlice }}
    </ComponentOutput>
  </div>
</template>
