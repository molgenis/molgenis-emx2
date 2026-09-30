<script lang="ts" setup>
import { ref, computed, useTemplateRef, onMounted, watch } from "vue";
import { useEventListener } from "@vueuse/core";

import {
  select,
  selectAll,
  scaleBand,
  axisBottom,
  max,
  min,
  scaleLinear,
  axisLeft,
} from "d3";
const d3 = {
  select,
  selectAll,
  scaleBand,
  axisBottom,
  max,
  min,
  scaleLinear,
  axisLeft,
};

import ChartTitle from "../ChartTitle.vue";

import {
  breakXAxisLabels,
  generateAxisTickData,
  newNumericAxisGenerator,
  newCategoricalAxisGenerator,
} from "../../../utils/viz";

import type {
  IStatisticalCharts,
  IChartData,
  IChartPalette,
} from "../../../../types/cms.ts";

import type {
  NumericAxisTickData,
  CategoricalAxisTickData,
} from "../../../../types/viz";

const props = withDefaults(defineProps<IStatisticalCharts>(), {
  chartWidth: 300,
  chartHeight: 300,
  topMargin: 10,
  rightMargin: 10,
  bottomMargin: 70,
  leftMargin: 45,
  fillColor: "#014f9e",
  hoverFillColor: "#53a9ff",
  enableHoverEvents: true,
  enableClickEvents: false,
  enableAnimations: true,
});

const emits = defineEmits(["column-clicked"]);

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // receives d3.select
const chartArea = ref(); // receives d3.select

const width = ref<number>(0);
const height = computed<number>(() => {
  return props.chartHeight - props.topMargin - internalBottomMargin.value;
});

const internalBottomMargin = computed<number>(() => {
  return props.xAxisTitle || props.breakXAxisTickLabelsAt
    ? props.bottomMargin
    : 25;
});

const yAxisData = computed<NumericAxisTickData>(() => {
  const ticks: NumericAxisTickData = {
    ...generateAxisTickData(props.chartData as IChartData[], "yValue"),
  };

  if (props.yAxisMaxValue) {
    ticks.limit = props.yAxisMaxValue;
  }

  if (props.yAxisTicks) {
    ticks.ticks = props.yAxisTicks;
  }

  if (props.yAxisMinValue) {
    ticks.min = props.yAxisMinValue;
  }

  return ticks;
});

const hasNegativeValues = computed<boolean>(() => {
  return yAxisData.value?.min < 0 || false;
});

const xAxisData = computed<CategoricalAxisTickData>(() => {
  const data = { count: 0, domains: [] as string[] };
  if (props.chartData) {
    const values = props.chartData.map((row: IChartData) => row.xValue);
    data.count = values.length;
    data.domains = values;
  }
  return data;
});

const xScale = computed(() => {
  return newCategoricalAxisGenerator({
    domains: xAxisData.value.domains,
    rangeEnd: width.value,
  });
});

const yScale = computed(() => {
  const domainMin = yAxisData.value.min < 0 ? yAxisData.value.min : 0;
  return newNumericAxisGenerator({
    domainLimit: yAxisData.value.limit,
    domainMin: domainMin,
    rangeStart: height.value,
  });
});

const colorPalette = computed<IChartPalette>(() => {
  const mappings = xAxisData.value.domains.map((value: string) => {
    const color = props.colorPalette
      ? props.colorPalette[value as unknown as number]
      : props.fillColor;
    return [value, color];
  });
  return Object.fromEntries(mappings);
});

function renderChartAxes() {
  if (chartArea.value) {
    const xAxis = d3.axisBottom(xScale.value);
    const yAxis = d3.axisLeft(yScale.value).tickValues(yAxisData.value.ticks);
    const chartAxisGroup = chartArea.value.select("g.axes");
    chartAxisGroup.select(".x-axis").call(xAxis);
    chartAxisGroup.select(".y-axis").call(yAxis);
  }

  if (props.breakXAxisTickLabelsAt) {
    breakXAxisLabels(svg.value, props.breakXAxisTickLabelsAt);
  }
}

function onMouseOver(event: Event) {
  const rect = event.target as HTMLElement;
  const text = rect.nextSibling as HTMLElement;
  rect.style.fill = props.hoverFillColor;
  text.style.opacity = "1";
}

function onMouseOut(event: Event, row: IChartData) {
  const rect = event.target as HTMLElement;
  const text = rect.nextSibling as HTMLElement;
  rect.style.fill = colorPalette.value[
    row.xValue as keyof IChartPalette
  ] as string;
  text.style.opacity = "0";
}

function renderColumns() {
  const chartColumnArea = chartArea.value.select("g.columns");
  const columns = chartColumnArea.selectAll("rect").data(props.chartData);

  let rectElem = columns;
  if (props.enableAnimations) {
    rectElem = columns
      .attr("y", yScale.value(0))
      .attr("height", 0)
      .transition()
      .delay(100)
      .duration(500);
  }

  rectElem
    .attr("y", (row: IChartData) => {
      const yValue = parseFloat(row.yValue);
      const yScaleValue = Math.max(0, yValue);
      return yScale.value(yScaleValue);
    })
    .attr("height", (row: IChartData) => {
      const yValue = parseFloat(row.yValue);
      const yScaleValue = yScale.value(yValue) - yScale.value(0);
      return Math.abs(yScaleValue);
    });

  if (props.enableHoverEvents) {
    columns
      .style("cursor", "pointer")
      .on("mouseover", onMouseOver)
      .on("mouseout", (event: Event, row: IChartData) =>
        onMouseOut(event, row)
      );
  }

  if (props.enableClickEvents) {
    columns
      .style("cursor", "pointer")
      .on("click", (_: Event, row: IChartData) => {
        emits("column-clicked", row);
      });
  }
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");

  width.value =
    (parentElem.value?.offsetWidth || props.chartWidth) -
    props.leftMargin -
    props.rightMargin;

  renderChartAxes();
  renderColumns();
}

onMounted(() => {
  renderChart();
  useEventListener("resize", renderChart);
});

watch(
  () => [props.chartData, props.x, props.y, props.xAxisTitle, props.yAxisTitle],
  () => {
    renderChart();
  },
  { deep: true }
);
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-full chart_layout_default">
    <ChartTitle
      :title="(chartTitle as string)"
      :description="chartDescription"
      style="grid-area: context"
    />
    <div style="grid-area: chart">
      <svg
        :id="id"
        :width="width"
        :height="props.chartHeight"
        preserve-aspect-ratio="xMinYMin"
        :viewBox="`0 0 ${width + leftMargin} ${props.chartHeight}`"
      >
        <g
          class="chart-area"
          :transform="`translate(${leftMargin}, ${topMargin})`"
        >
          <g class="columns">
            <g
              v-for="row in chartData"
              class="rect-group"
              :key="row.xValue"
              :data-x="row.xValue"
              :data-y="row.yValue"
            >
              <rect
                class="column"
                :width="xScale.bandwidth()"
                :fill="(colorPalette[row.xValue as keyof IChartPalette] as string)"
                :x="xScale(row.xValue)"
                :stroke="strokeColor ? strokeColor : undefined"
                :stroke-width="strokeColor ? '1' : undefined"
              />

              <text
                class="fill-chart-text text-body-base"
                text-anchor="middle"
                :opacity="0"
                :x="xScale(row.xValue)"
                :y="yScale(parseInt(row.yValue))"
                :dx="xScale.bandwidth() / 2"
                :dy="parseInt(row.yValue) < 0 ? '1.15em' : '-0.35em'"
              >
                {{ row.yLabel || row.yValue }}
              </text>
            </g>
          </g>
          <g
            class="axes [&_text]:fill-chart-text [&_text]:text-body-sm [&_line]:stroke-chart-paths [&_path]:stroke-chart-paths"
          >
            <g class="x-axis-zero" v-if="hasNegativeValues">
              <line
                class="domain"
                stroke="black"
                :x1="0"
                :x2="width"
                :y1="yScale(0)"
                :y2="yScale(0)"
              ></line>
            </g>
            <g
              class="x-axis"
              :transform="`translate(0,${height})`"
              :class="{
                '[&_path]:hidden [&_line]:hidden': hasNegativeValues,
              }"
            ></g>
            <g class="y-axis"></g>
          </g>
        </g>
        <g class="titles">
          <text
            v-if="xAxisTitle"
            class="fill-chart-text text-body-base"
            :x="width / 2"
            :y="
              yAxisData.min < 0
                ? yScale(yAxisData.min) + internalBottomMargin
                : yScale(0) + internalBottomMargin * 0.9
            "
            dy="0.5em"
          >
            {{ xAxisTitle }}
          </text>
          <text
            v-if="yAxisTitle"
            class="fill-chart-text -rotate-90 text-body-base"
            text-anchor="middle"
            :x="-height * 0.55"
            :y="leftMargin / 2"
            dy="-0.8em"
          >
            {{ yAxisTitle }}
          </text>
        </g>
      </svg>
    </div>
  </div>
</template>
