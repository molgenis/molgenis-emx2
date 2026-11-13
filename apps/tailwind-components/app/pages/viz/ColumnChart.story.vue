<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";

import type { IChartData } from "../../../types/cms.ts";

const recruitmentDataExample: IChartData[] = [
  { id: "value-jan", xValue: "Jan", yValue: "18" },
  { id: "value-feb", xValue: "Feb", yValue: "33" },
  { id: "value-mar", xValue: "Mar", yValue: "56" },
  { id: "value-apr", xValue: "Apr", yValue: "75" },
  { id: "value-may", xValue: "May", yValue: "22" },
  { id: "value-jun", xValue: "Jun", yValue: "10" },
];

const recruitmentTargetExample: IChartData[] = [
  { id: "value-jan", xValue: "Jan", yValue: "-32", fillColor: "#e14f62" },
  { id: "value-feb", xValue: "Feb", yValue: "17", yLabel: "+17" },
  { id: "value-mar", xValue: "Mar", yValue: "6", yLabel: "+6" },
  { id: "value-apr", xValue: "Apr", yValue: "-25", fillColor: "#e14f62" },
  { id: "value-may", xValue: "May", yValue: "-28", fillColor: "#e14f62" },
  { id: "value-jun", xValue: "Jun", yValue: "-40", fillColor: "#e14f62" },
];

const recruitmentByGender: IChartData[] = [
  {
    id: "center-a-female",
    xValue: "Female",
    yValue: "15",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-a-male",
    xValue: "Male",
    yValue: "13",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-a-other",
    xValue: "Other",
    yValue: "5",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-b-female",
    xValue: "Female",
    yValue: "13",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-b-male",
    xValue: "Male",
    yValue: "2",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-b-other",
    xValue: "Other",
    yValue: "1",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-c-female",
    xValue: "Female",
    yValue: "6",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-c-male",
    xValue: "Male",
    yValue: "7",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-c-other",
    xValue: "Other",
    yValue: "2",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-d-female",
    xValue: "Female",
    yValue: "2",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-d-male",
    xValue: "Male",
    yValue: "2",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-d-other",
    xValue: "Other",
    yValue: "0",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-e-female",
    xValue: "Female",
    yValue: "1",
    primaryGroupValue: "Center E",
  },
  {
    id: "center-e-male",
    xValue: "Male",
    yValue: "0",
    primaryGroupValue: "Center E",
  },
  {
    id: "center-e-other",
    xValue: "Other",
    yValue: "0",
    primaryGroupValue: "Center E",
  },
];

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
        :yAxisMaxValue="100"
        :yAxisTicks="[0, 25, 50, 75, 100]"
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
        :yAxisMinValue="-50"
        :yAxisMaxValue="20"
        :yAxisTicks="[-50, -40, -30, -20, -10, 0, 10, 20]"
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
