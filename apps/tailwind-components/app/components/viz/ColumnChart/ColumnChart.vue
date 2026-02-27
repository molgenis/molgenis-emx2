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

import type {
  DatasetRow,
  ColumnCharts,
  ColorPalette,
  NumericAxisTickData,
  CategoricalAxisTickData,
} from "../../../../types/viz";
import {
  breakXAxisLabels,
  generateAxisTickData,
  newNumericAxisGenerator,
  newCategoricalAxisGenerator,
} from "../../../utils/viz";

const props = withDefaults(defineProps<ColumnCharts>(), {
  width: 300,
  height: 300,
  marginTop: 10,
  marginRight: 10,
  marginBottom: 70,
  marginLeft: 45,
  columnColor: "#014f9e",
  columnColorOnHover: "#53a9ff",
  hoverEventsAreEnabled: true,
  clickEventsAreEnabled: false,
  animationsAreEnabled: true,
});

const emits = defineEmits(["column-clicked"]);

const parentElem = ref<HTMLElement>();
const container = useTemplateRef("container");

const svg = ref(); // receives d3.select("svg");
const chartArea = ref(); // receives d3.select("svg g.chart-area");
const width = ref<number>(props.width);
const height = ref<number>(props.height);
const internalLeftMargin = ref<number>(0);
const internalBottomMargin = ref<number>(0);

const xScale = ref();
const yScale = ref();

const yAxisData = computed<NumericAxisTickData>(() => {
  const autoTickData = generateAxisTickData(props.data, props.yvar);
  const ticks: NumericAxisTickData = { ...autoTickData };

  if (props.ymax) {
    ticks.limit = props.ymax;
  }

  if (props.yTickValues) {
    ticks.ticks = props.yTickValues;
  }

  return ticks;
});

const xAxisData = computed<CategoricalAxisTickData>(() => {
  const data = { count: 0, domains: [] as string[] };
  if (props.data) {
    const values = props.data.map((row: DatasetRow) => row[props.xvar]);
    data.count = values.length;
    data.domains = values;
  }
  return data;
});

const colorPalette = computed<ColorPalette>(() => {
  const mappings = xAxisData.value.domains.map((value: string) => {
    const color = props.colorPalette
      ? props.colorPalette[value]
      : props.columnColor;
    return [value, color];
  });
  return Object.fromEntries(mappings);
});

function setChartDimensions() {
  parentElem.value = container.value?.parentNode as HTMLElement;
  internalLeftMargin.value = props.yAxisLabel ? props.marginLeft : 25;
  internalBottomMargin.value =
    props.xAxisLabel || props.breakXAxisLabelsAt ? props.marginBottom : 25;

  height.value = props.height - props.marginTop - internalBottomMargin.value;
  width.value =
    (parentElem.value?.offsetWidth || props.width) -
    internalLeftMargin.value -
    props.marginRight;
}

function renderChartAxes() {
  const chartAxisGroup = chartArea.value?.select("g.axes");
  chartAxisGroup?.selectAll("*").remove();

  xScale.value = newCategoricalAxisGenerator({
    domains: xAxisData.value.domains,
    rangeEnd: width.value,
  });
  yScale.value = newNumericAxisGenerator({
    domainLimit: yAxisData.value.limit,
    rangeStart: height.value,
  });

  const xAxis = d3.axisBottom(xScale.value);
  const yAxis = d3.axisLeft(yScale.value).tickValues(yAxisData.value.ticks);

  chartAxisGroup.append("g").attr("class", "y-axis").call(yAxis);
  chartAxisGroup
    .append("g")
    .attr("class", "x-axis")
    .attr("transform", `translate(0,${height.value})`)
    .call(xAxis);

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
  rect.style.fill = props.columnColorOnHover;
  text.style.opacity = "1";
}

function onMouseOut(event: Event, row: DatasetRow) {
  const rect = event.target as HTMLElement;
  const text = rect.nextSibling as HTMLElement;
  rect.style.fill = colorPalette.value[row[props.xvar]] as string;
  text.style.opacity = "0";
}

function renderColumns() {
  const chartColumnArea = chartArea.value.select("g.columns");
  chartColumnArea.selectAll("*").remove();

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

  if (props.columnBorderColor) {
    columns.attr("stroke", props.columnBorderColor).attr("stroke-width", "1");
  }

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
      .style("cursor", "pointer")
      .on("mouseover", onMouseOver)
      .on("mouseout", (event: Event, row: DatasetRow) =>
        onMouseOut(event, row)
      );
  }

  if (props.clickEventsAreEnabled) {
    columns
      .style("cursor", "pointer")
      .on("click", (_: Event, row: DatasetRow) => {
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
  if (props.xAxisLabel) {
    axisTitles
      .append("text")
      .attr("class", "fill-chart-text text-body-base")
      .attr("x", width.value / 2)
      .attr("y", () => {
        if (yAxisData.value.min < 0) {
          return yScale.value(yAxisData.value.min) + internalBottomMargin.value;
        } else {
          return yScale.value(0) + internalBottomMargin.value * 0.9;
        }
      })
      .attr("dy", "0.5em")
      .text(props.xAxisLabel);
  }

  if (props.yAxisLabel) {
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
  () => [props.data, props.xvar, props.yvar],
  () => {
    renderChart();
  },
  { deep: true }
);
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-full chart_layout_default">
    <ChartContext
      :title="title"
      :description="description"
      style="grid-area: context"
    />
    <div style="grid-area: chart">
      <svg
        :id="id"
        width="100%"
        :height="props.height"
        preserve-aspect-ratio="xMinYMin"
        :viewBox="`0 0 ${width + internalLeftMargin} ${props.height}`"
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
