<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 9 },
  { name: "Group E", value: 12 },
  { name: "Group F", value: 41 },
];

const clickedElem = ref<Record<string, number>>();
</script>

<template>
  <div class="h-auto">
    <ColumnChart
      id="column-chart-demo"
      title="Participants by experimental group"
      description="Participants (n=214) were randomised into one of four groups"
      :data="data"
      xvar="name"
      yvar="value"
      x-axis-label="Experimental group"
      y-axis-label="Number of participants"
      :click-events-are-enabled="true"
      :hover-events-are-enabled="true"
      @column-clicked="clickedElem = $event"
    />
    <ComponentOutput class="mt-2.5">
      Clicked element: {{ clickedElem }}
    </ComponentOutput>
  </div>
</template>
