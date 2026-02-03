<script setup lang="ts">
import { computed, useTemplateRef, ref, onMounted } from "vue";

import ChartContext from "../ChartContext.vue";
import {
  select,
  selectAll,
  scaleOrdinal,
  pie,
  arc,
  schemeBlues,
  sort,
} from "d3";
const d3 = { select, selectAll, scaleOrdinal, pie, arc, schemeBlues, sort };

import type { PieCharts } from "../../../../types/viz";

const props = withDefaults(defineProps<PieCharts>(), {
  width: 300,
  height: 300,
  showValues: true,
  showValuesAsPercentages: true,
  asDonutChart: false,
  pieChartIsCentered: false,
  hoverEventsAreEnabled: false,
  clickEventsAreEnabled: false,
  chartLegendIsEnabled: true,
  legendIsStacked: false,
  legendPosition: "top",
  legendClickEventsAreEnabled: false,
  legendHoverEventsAreEnabled: false,
});

const container = useTemplateRef("container");
const svg = ref();
const chartArea = ref();

function selectPrimaryChartElements() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = d3.select(`#${props.id}-chart-area`);
}

const width = computed<number>(() => {
  const parent = container.value?.parentNode as HTMLElement;
  return parent?.offsetWidth || props.width;
});

const viewBox = computed(() => {
  return `0 0 ${width.value} ${props.height}`;
});

function renderChart() {
  selectPrimaryChartElements();
}

onMounted(() => {
  renderChart();
});
</script>

<template>
  <div ref="container">
    <ChartContext :title="title" :description="description" />
    <svg
      :id="id"
      width="100%"
      height="100%"
      preserve-aspect-ratio="xMinYMin"
      :view-box="viewBox"
      :class="{
        'm-auto': pieChartIsCentered,
      }"
    >
      <g :id="`${id}-chart-area`" ref="chartArea">
        <g :id="`${id}-pie-slices`"></g>
        <g :id="`${id}-pie-labels`"></g>
      </g>
    </svg>
  </div>
</template>
