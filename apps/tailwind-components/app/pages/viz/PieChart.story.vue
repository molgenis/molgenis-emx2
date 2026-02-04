<script lang="ts" setup>
import { ref } from "vue";
import PieChart from "../../components/viz/PieChart/PieChart.vue";
import ComponentOutput from "~/components/story/ComponentOutput.vue";

const data = {
  "Group A": 0.5,
  "Group B": 0.2,
  "Group C": 0.3,
};

const clickedSlice = ref<Record<string, number>>();
</script>
<template>
  <div class="h-auto w-auto">
    <PieChart
      id="pie-chart-demo"
      title="Group assignment"
      description="Total assignments by group"
      :data="data"
      :as-donut-chart="true"
      :click-events-are-enabled="true"
      :show-labels="true"
      :show-values="false"
      @slice-clicked="clickedSlice = $event"
    />
  </div>
  <ComponentOutput> Clicked segment: {{ clickedSlice }} </ComponentOutput>
</template>
