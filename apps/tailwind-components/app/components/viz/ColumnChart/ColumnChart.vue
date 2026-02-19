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

import ChartContext from "../ChartContext.vue";
import ChartLegend from "../ChartLegend/ChartLegend.vue";

import type {
  ColumnCharts,
  ColorPalette,
  NumericAxisTickData,
  CategoricalAxisTickData,
} from "../../../../types/viz";
import {
  setChartLegendLayoutCss,
  breakXAxisLabels,
  generateAxisTickData,
} from "../../../utils/viz";

type DatasetRow = Record<string, any>;

const props = withDefaults(defineProps<ColumnCharts>(), {
  width: 250,
  height: 250,
  marginTop: 10,
  marginRight: 10,
  marginBottom: 60,
  marginLeft: 60,
  columnColor: "#6C85B5",
  columnColorOnHover: "#163D89",
  hoverEventsAreEnabled: true,
  clickEventsAreEnabled: false,
  animationsAreEnabled: true,
  legendIsEnabled: false,
  legendIsStacked: false,
  legendPosition: "top",
  legendHoverEventsAreEnabled: false,
  showGridLines: false,
});

const emits = defineEmits(["column-clicked"]);

const parentElem = ref<HTMLElement>();
const container = useTemplateRef("container");

const svg = ref();
const chartArea = ref();
const width = ref<number>(props.width);
const height = ref<number>(props.height);
const internalLeftMargin = ref<number>(0);
const internalBottomMargin = ref<number>(0);

const xScale = ref();
const yScale = ref();
const xAxis = ref();
const yAxis = ref();

const yAxisData = computed<NumericAxisTickData>(() => {
  const autoTickData = generateAxisTickData(props.data, props.yvar);
  const ticks: NumericAxisTickData = {
    limit: autoTickData.limit,
    ticks: autoTickData.ticks,
  };

  if (props.ymax) {
    ticks.limit = props.ymax;
  }

  if (props.yTickValues) {
    ticks.ticks = props.yTickValues;
  }

  return ticks;
});

const xAxisData = computed<CategoricalAxisTickData>(() => {
  const data = { count: 0, domains: [] };
  if (props.data) {
    const values = props.data.map((row: DatasetRow) => row[props.xvar]);
    data.count = values.length;
    data.domains = values;
  }
  return data;
});

// define values that might become props later
const columnPaddingInner: number = 0.2;
const columnPaddingOuter: number = 0.2;
const columnAlign: number = 0.5;

const chartLayoutCss = computed<string>(() => {
  return setChartLegendLayoutCss(props.legendIsEnabled, props.legendPosition);
});

const colorPalette = computed<ColorPalette>(() => {
  const domain = props.data.map((row: DatasetRow) => row[props.xvar]);
  const mappings = domain.map((value: string) => {
    const color = props.colorPalette
      ? props.colorPalette[value]
      : props.columnColor;
    return [value, color];
  });
  return Object.fromEntries(mappings);
});

function setChartDimensions() {
  internalLeftMargin.value = props.yAxisLabel ? props.marginLeft : 25;
  internalBottomMargin.value = props.xAxisLabel ? props.marginBottom : 25;

  parentElem.value = container.value?.parentNode as HTMLElement;
  width.value =
    (parentElem.value?.offsetWidth || props.width) -
    internalLeftMargin.value -
    props.marginRight;
  height.value = props.height - props.marginTop - internalBottomMargin.value;
}

function createYAxisGenerator() {
  return d3
    .scaleLinear()
    .domain([0, yAxisData.value.limit])
    .range([height.value, 0])
    .nice();
}

function createXAxisGenerator() {
  return d3
    .scaleBand()
    .range([0, width.value])
    .domain(props.data.map((row: DatasetRow) => row[props.xvar]))
    .paddingInner(columnPaddingInner)
    .paddingOuter(columnPaddingOuter)
    .align(columnAlign)
    .round(true);
}

function renderChartAxes() {
  const chartAxisGroup = chartArea.value.select("g.axes");
  chartAxisGroup.selectAll("*").remove();

  // initialise generates and bind axes to chart
  xScale.value = createXAxisGenerator();
  yScale.value = createYAxisGenerator();

  if (props.showGridLines) {
    const gridlines = chartAxisGroup.append("g").attr("class", "gridlines");
    const xGridlines = gridlines.append("g").attr("class", "x");
    const yGridlines = gridlines.append("g").attr("class", "y");

    xGridlines
      .selectAll("line")
      .data(yAxisData.value.ticks)
      .join("line")
      .attr("x1", 0)
      .attr("x2", width.value)
      .attr("y1", (tick: number) => yScale.value(tick))
      .attr("y2", (tick: number) => yScale.value(tick))
      .attr("class", "stroke-chart-gridlines");

    yGridlines
      .selectAll("line")
      .data(xAxisData.value.domains)
      .join("line")
      .attr(
        "x1",
        (domain: string) => xScale.value(domain) + xScale.value.bandwidth() / 2
      )
      .attr(
        "x2",
        (domain: string) => xScale.value(domain) + xScale.value.bandwidth() / 2
      )
      .attr("y1", 0)
      .attr("y2", height.value)
      .attr("class", "stroke-chart-gridlines");
  }

  xAxis.value = d3.axisBottom(xScale.value);
  yAxis.value = d3.axisLeft(yScale.value).tickValues(yAxisData.value.ticks);

  chartAxisGroup.append("g").attr("class", "y-axis").call(yAxis.value);

  chartAxisGroup
    .append("g")
    .attr("class", "x-axis")
    .attr("transform", `translate(0,${height.value})`)
    .call(xAxis.value);

  // apply tailwind styles
  chartAxisGroup.selectAll(".tick line").attr("class", "stroke-chart-paths");
  chartAxisGroup.selectAll("path").attr("class", "stroke-chart-paths");
  chartAxisGroup
    .selectAll(".tick text")
    .attr("class", "fill-chart-text text-body-sm");

  if (props.breakXAxisLabelsAt) {
    breakXAxisLabels(svg.value, props.breakXAxisLabelsAt);
  }
}

function onMouseOver(event: Event) {
  const rect = event.target as HTMLElement;
  const text = rect.nextSibling as HTMLElement;
  rect.style.cursor = "pointer";
  rect.style.fill = props.columnColorOnHover;
  text.style.opacity = "1";
}

function onMouseOut(event: Event, row: DatasetRow) {
  const rect = event.target as HTMLElement;
  const text = rect.nextSibling as HTMLElement;
  rect.style.cursor = "default";
  rect.style.fill = colorPalette.value[row[props.xvar]] as string;
  text.style.opacity = "0";
}

function renderColumns() {
  const chartColumnArea = chartArea.value.select("g.columns");
  chartColumnArea.selectAll("*").remove();

  // group svg elements by data point: rect+text
  const groups = chartColumnArea
    .selectAll("g")
    .data(props.data)
    .join("g")
    .attr("data-x", (row: DatasetRow) => row[props.xvar])
    .attr("data-y", (row: DatasetRow) => row[props.yvar]);

  const columns = groups
    .append("rect")
    .attr("fill", (row: DatasetRow) => colorPalette.value[row[props.xvar]])
    .attr("width", xScale.value.bandwidth())
    .attr("x", (row: DatasetRow) => xScale.value(row[props.xvar]));

  if (props.animationsAreEnabled) {
    columns
      .attr("y", yScale.value(0))
      .attr("height", 0)
      .transition()
      .delay(300)
      .duration(500)
      .attr("y", (row: DatasetRow) => {
        return yScale.value(Math.max(0, row[props.yvar]));
      })
      .attr("height", (row: DatasetRow) => {
        return Math.abs(yScale.value(row[props.yvar]) - yScale.value(0));
      });
  } else {
    columns
      .attr("y", (row: DatasetRow) => {
        return yScale.value(Math.max(0, row[props.yvar]));
      })
      .attr("height", (row: DatasetRow) => {
        return Math.abs(yScale.value(row[props.yvar]) - yScale.value(0));
      });
  }

  if (props.hoverEventsAreEnabled) {
    columns
      .on("mouseover", onMouseOver)
      .on("mouseout", (event: Event, row: DatasetRow) =>
        onMouseOut(event, row)
      );
  }

  if (props.clickEventsAreEnabled) {
    columns.on("click", (_: Event, row: DatasetRow) => {
      emits("column-clicked", row);
    });
  }

  groups
    .append("text")
    .attr("class", "fill-chart-text text-body-base")
    .attr("text-anchor", "middle")
    .style("opacity", 0)
    .attr("x", (row: DatasetRow) => xScale.value(row[props.xvar]))
    .attr("y", (row: DatasetRow) => yScale.value(row[props.yvar]))
    .attr("dx", xScale.value.bandwidth() / 2)
    .attr("dy", "-0.35em")
    .text((row: DatasetRow) => row[props.yvar]);
}

function renderChartAxisTitles() {
  const axisTitles = svg.value.select("g.titles");
  axisTitles.selectAll("*").remove();
  if (props.xAxisLabel && props.xAxisLabel !== "") {
    axisTitles
      .append("text")
      .attr("class", "fill-chart-text text-body-base")
      .attr("x", width.value / 2)
      .attr("y", () => {
        const minY = Math.min(
          props.data.map((row: DatasetRow) => row[props.yvar])
        );
        if (minY < 0) {
          return yScale.value(minY) + internalBottomMargin.value;
        } else {
          return yScale.value(0) + internalBottomMargin.value * 0.8;
        }
      })
      .attr("dy", "0.5em")
      .text(props.xAxisLabel);
  }

  if (props.yAxisLabel && props.yAxisLabel !== "") {
    axisTitles
      .append("text")
      .attr("class", "fill-chart-text -rotate-90 text-body-base")
      .attr("text-anchor", "middle")
      .attr("x", -height.value * 0.55)
      .attr("y", internalLeftMargin.value / 2)
      .attr("dy", "-0.8em")
      .text(props.yAxisLabel);
  }
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");

  setChartDimensions();
  renderChartAxes();
  renderColumns();

  if (props.xAxisLabel || props.yAxisLabel) {
    renderChartAxisTitles();
  }
}

onMounted(() => {
  renderChart();

  useEventListener("resize", renderChart);
});

watch(
  () => props,
  () => {
    renderChart();
  },
  { deep: true }
);
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-full" :class="chartLayoutCss">
    <ChartContext
      :title="title"
      :description="description"
      style="grid-area: context"
    />
    <ChartLegend
      v-if="legendIsEnabled"
      :legend-id="id"
      :data="colorPalette"
      :stack-legend="legendIsStacked"
      :enable-hovering="legendHoverEventsAreEnabled"
      style="grid-area: legend"
      class="mb-2.5"
      :class="{
        'm-auto': legendIsEnabled && legendPosition === 'bottom',
      }"
    />
    <div style="grid-area: chart">
      <svg
        :id="id"
        width="100%"
        :height="props.height"
        preserve-aspect-ratio="xMinYMin"
        :view-box="`0 0 ${props.width} ${props.height}`"
      >
        <g
          class="chart-area"
          :transform="`translate(${internalLeftMargin}, ${marginTop})`"
        >
          <g class="axes"></g>
          <g class="columns"></g>
        </g>
        <g class="titles"></g>
      </svg>
    </div>
  </div>
</template>
