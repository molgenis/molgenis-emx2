<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";

import {
  recruitmentByGender,
  recruitmentDataExample,
  recruitmentTargetExample,
} from "../../utils/viz/data.ts";

const chartClick1 = ref<Record<string, number>>();
const chartClick2 = ref<Record<string, number>>();
const chartClick3 = ref<Record<string, number>>();
</script>

<template>
  <div class="grid grid-cols-1 gap-5 h-auto">
    <div>
      <ColumnChart
        id="column-chart-demo-1"
        :chartType="{ name: 'ColumnChart' }"
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
        :chartType="{ name: 'ColumnChart' }"
        chartTitle="Participant recruitment goals"
        chartDescription="The difference of participant recruitment in relation to the target per month (n=50). This is chart that demonstrates negative numbers and custom colors."
        :chartData="recruitmentTargetExample"
        x="xValue"
        y="yValue"
        yAxisTitle="recruitment difference"
        :enableAnimations="true"
        :enableClickEvents="true"
        :enableHoverEvents="true"
        @columnClicked="chartClick2 = $event"
      />
      <ComponentOutput class="my-2.5">
        Clicked element: {{ chartClick2 }}
      </ComponentOutput>
    </div>
    <div>
      <ColumnChart
        id="column-chart-demo-3"
        :chartType="{ name: 'Grouped column chart' }"
        chartTitle="Participant recruitment by age group and center"
        :chartData="recruitmentByGender"
        x="xValue"
        y="yValue"
        primaryGroupValue="primaryGroupValue"
        yAxisTitle="Number of participants"
        :enableAnimations="true"
        :enableClickEvents="true"
        :enableHoverEvents="true"
        :legendIsEnabled="true"
        :legendPosition="{ name: 'top' }"
        @columnClicked="chartClick3 = $event"
      />
      <ComponentOutput class="my-2.5">
        Clicked element: {{ chartClick3 }}
      </ComponentOutput>
    </div>
  </div>
</template>
