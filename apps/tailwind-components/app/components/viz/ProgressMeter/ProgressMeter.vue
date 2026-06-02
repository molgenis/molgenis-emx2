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

import type {
  ProgressMeter,
  DatasetRow,
  ColorPalette,
  CategoricalAxisTickData,
} from "../../../../types/viz";

const props = withDefaults(defineProps<ProgressMeter>(), {
  color: "#0173e4",
  width: 500,
  height: 225,
  marginTop: 5,
  marginRight: 10,
  marginBottom: 5,
  marginLeft: 2,
  animationsAreEnabled: true,
  showValuesAsPercentages: true,
});

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // receives d3.select
const chartArea = ref(); // receives d3.select

const width = ref<number>(props.width);

const yAxisData = computed<CategoricalAxisTickData>(() => {
  const data = { count: 0, domains: [] as string[] };
  if (props.data) {
    const values = props.data.map((row: DatasetRow) => row[props.labels]);
    data.count = values.length;
    data.domains = values;
  }
  return data;
});

const xScale = computed(() => {
  return newNumericAxisGenerator({
    domainMin: 0,
    domainLimit: Math.max(
      ...props.data.map((row: DatasetRow) => row[props.totals])
    ),
    rangeStart: 0,
    rangeEnd: width.value,
  });
});

const yScale = computed(() => {
  return newCategoricalAxisGenerator({
    domains: yAxisData.value.domains,
    rangeStart: 0,
    rangeEnd: props.height,
    paddingInner: 0.55,
  });
});

const colorPalette = computed<ColorPalette>(() => {
  const mappings = yAxisData.value.domains.map((value: string) => {
    const color = props.colorPalette ? props.colorPalette[value] : props.color;
    return [value, color];
  });
  return Object.fromEntries(mappings);
});

function renderBar(isResized: boolean = false) {
  const bar = chartArea.value.selectAll("rect.bar").data(props.data);

  if (props.animationsAreEnabled) {
    bar
      .attr(
        "width",
        isResized ? (row: DatasetRow) => xScale.value(row[props.values]) : 0
      )
      .transition()
      .duration(1000)
      .attr("width", (row: DatasetRow) => xScale.value(row[props.values]));
  } else {
    bar.attr("width", (row: DatasetRow) => xScale.value(row[props.values]));
  }
}

function renderChart(isResized: boolean = false) {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select(".chart-area");

  width.value =
    (parentElem.value?.offsetWidth || props.width) -
    props.marginLeft -
    props.marginRight;

  renderBar(isResized);
}

onMounted(() => {
  renderChart();
  useEventListener("resize", () => renderChart(true));
});

watch(
  () => [props.data],
  () => renderChart(),
  { deep: true }
);
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-full chart_layout_default">
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
        :viewBox="`0 0 ${width + marginRight} ${height}`"
      >
        <g
          class="chart-area"
          :transform="`translate(${marginLeft},${marginTop})`"
        >
          <g
            v-for="row in data"
            :data-x="row[props.values]"
            :data-y="row[props.labels]"
          >
            <text
              :y="(yScale(row[labels]) as number) - marginTop - 2"
              class="fill-chart-text"
            >
              <tspan :x="xScale(0)">{{ row[labels] }}</tspan>
              <tspan
                :x="
                  xScale(row[totals]) -
                  marginRight -
                  marginLeft -
                  (showValuesAsPercentages ? 18 : 2)
                "
              >
                {{ showValuesAsPercentages ? `${row[values]}%` : row[values] }}
              </tspan>
            </text>
            <rect
              class="bar-outline"
              fill="none"
              :stroke="colorPalette[row[labels]]"
              stroke-width="1"
              :x="xScale(0)"
              :y="yScale(row[labels])"
              :width="xScale(row[totals])"
              :height="yScale.bandwidth()"
            />
            <rect
              class="bar"
              :fill="colorPalette[row[labels]]"
              :stroke="colorPalette[row[labels]]"
              stroke-width="1"
              :x="xScale(0)"
              :y="yScale(row[labels])"
              :height="yScale.bandwidth()"
            />
          </g>
        </g>
      </svg>
    </div>
  </div>
</template>
