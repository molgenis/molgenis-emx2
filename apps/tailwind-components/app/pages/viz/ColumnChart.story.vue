<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";
import type { IStatisticalChartData } from "../../../types/cms.ts";

const data = [
  { id: "value-0", xValue: "Group A", yValue: "42" },
  { id: "value-1", xValue: "Group B", yValue: "31" },
  { id: "value-2", xValue: "Group C", yValue: "82" },
  { id: "value-3", xValue: "Group D", yValue: "9" },
  { id: "value-4", xValue: "Group E", yValue: "12" },
  { id: "value-5", xValue: "Group F", yValue: "41" },
] as IStatisticalChartData[];

const clickedElem = ref<Record<string, number>>();
</script>

<template>
  <div class="h-auto">
    <ColumnChart
      id="column-chart-demo"
      chartTitle="Participants by experimental group"
      chartDescription="Participants (n=214) were randomised into one of four groups"
      :chartData="data"
      x="x value"
      y="y value"
      xAxisTitle="Experimental group"
      yAxisTitle="Number of participants"
      strokeColor="blue"
      :enableAnimations="true"
      :enableClickEvents="true"
      :enableHoverEvents="true"
      @column-clicked="clickedElem = $event"
    />
    <ComponentOutput class="mt-2.5">
      Clicked element: {{ clickedElem }}
    </ComponentOutput>
  </div>
</template>
