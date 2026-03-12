<script lang="ts" setup>
import { ref, computed, useTemplateRef, onMounted, watch } from "vue";
import { useEventListener } from "@vueuse/core";

import { select } from "d3";
const d3 = { select };

import ChartTitle from "../ChartTitle.vue";

import {
  newNumericAxisGenerator,
  newCategoricalAxisGenerator,
} from "../../../utils/viz";

import type { ProgressMeter } from "../../../../types/viz";

const props = withDefaults(defineProps<ProgressMeter>(), {
  color: "#0173e4",
  width: 500,
  height: 200,
  marginTop: 5,
  marginRight: 10,
  marginBottom: 5,
  marginLeft: 5,
  animationsAreEnabled: true,
});

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // receives d3.select
const chartArea = ref(); // receives d3.select

const width = ref<number>(0);

const xScale = computed(() => {
  return newNumericAxisGenerator({
    domainMin: 0,
    domainLimit: props.total,
    rangeStart: 0,
    rangeEnd: width.value,
  });
});

const yScale = computed(() => {
  return newCategoricalAxisGenerator({
    domains: [props.label],
    rangeStart: 0,
    rangeEnd: props.height,
  });
});

function renderBar() {
  const bar = chartArea.value.select("rect.bar");

  if (props.animationsAreEnabled) {
    bar
      .attr("width", 0)
      .transition()
      .delay(300)
      .duration(500)
      .attr("width", xScale.value(props.value));
  } else {
    bar.attr("width", xScale.value(props.value));
  }
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select(".chart-area");

  width.value =
    (parentElem.value?.offsetWidth || props.width) -
    props.marginLeft -
    props.marginRight;

  renderBar();
}

onMounted(() => {
  renderChart();
  useEventListener("resize", renderChart);
});

watch(
  () => [props.value, props.total],
  () => renderChart(),
  { deep: true }
);
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-ful chart_layout_default">
    <ChartTitle
      :title="title"
      :description="description"
      style="grid-area: context"
    />
    <div style="grid-area: chart">
      <svg
        :id="id"
        width="100%"
        height="100%"
        :viewBox="`0 0 ${width + marginRight} ${
          height + marginTop + marginBottom
        }`"
      >
        <g
          class="chart-area"
          :transform="`translate(${marginLeft},${marginTop})`"
        >
          <g>
            <text
              :y="(yScale(label) as number) - marginTop - 2"
              class="fill-chart-text"
            >
              <tspan :x="xScale(0)">{{ label }}</tspan>
              <tspan :x="xScale(total) - marginRight - marginLeft - 2">
                {{ value }}
              </tspan>
            </text>
            <rect
              class="bar-outline"
              fill="none"
              :stroke="color"
              stroke-width="2"
              :x="xScale(0)"
              :y="yScale(label)"
              :width="xScale(total)"
              height="25"
            />
            <rect
              class="bar"
              :fill="color"
              :stroke="color"
              stroke-width="2"
              :x="xScale(0)"
              :y="yScale(label)"
              height="25"
            />
          </g>
        </g>
      </svg>
    </div>
  </div>
</template>
