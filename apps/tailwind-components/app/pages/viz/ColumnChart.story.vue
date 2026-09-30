<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";

const recruitmentDataExample = [
  { id: "value-jan", xValue: "Jan", yValue: "18" },
  { id: "value-feb", xValue: "Feb", yValue: "33" },
  { id: "value-mar", xValue: "Mar", yValue: "56" },
  { id: "value-apr", xValue: "Apr", yValue: "75" },
  { id: "value-may", xValue: "May", yValue: "22" },
  { id: "value-jun", xValue: "Jun", yValue: "10" },
];

const data = [
  { id: "value-jan", xValue: "Jan", yValue: "-32" },
  { id: "value-feb", xValue: "Feb", yValue: "17", yLabel: "+17" },
  { id: "value-mar", xValue: "Mar", yValue: "6", yLabel: "+6" },
  { id: "value-apr", xValue: "Apr", yValue: "-25" },
  { id: "value-may", xValue: "May", yValue: "-28" },
  { id: "value-jun", xValue: "Jun", yValue: "-40" },
];

const chartClick1 = ref<Record<string, number>>();
const chartClick2 = ref<Record<string, number>>();
</script>

<template>
  <div class="grid grid-cols-1 gap-5 h-auto">
    <div>
      <ColumnChart
        id="column-chart-demo-1"
        chartTitle="Participants recruited by month"
        chartDescription="Participants (n=214) recruited from January 2026 to June 2026"
        :chartData="recruitmentDataExample"
        x="xValue"
        y="yValue"
        xAxisTitle="Experimental group"
        yAxisTitle="Number of participants"
        :enableAnimations="true"
        :enableClickEvents="true"
        :enableHoverEvents="true"
        @column-clicked="chartClick1 = $event"
      />
      <ComponentOutput class="my-2.5">
        Clicked element: {{ chartClick1 }}
      </ComponentOutput>
    </div>
    <div>
      <ColumnChart
        id="column-chart-demo-2"
        chartTitle="Participant recruitment goals"
        chartDescription="The difference of participant recruitment in relation to the target per month (n=50)"
        :chartData="data"
        x="xValue"
        y="yValue"
        yAxisTitle="recruitment difference"
        :yAxisMinValue="-50"
        :yAxisMaxValue="20"
        :yAxisTicks="[-50, -40, -30, -20, -10, 0, 10, 20]"
        :enableAnimations="true"
        :enableClickEvents="true"
        :enableHoverEvents="true"
        @column-clicked="chartClick2 = $event"
      />
      <ComponentOutput class="my-2.5">
        Clicked element: {{ chartClick2 }}
      </ComponentOutput>
    </div>
  </div>
</template>
