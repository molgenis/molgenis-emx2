<script setup lang="ts">
import {
  computed,
  useTemplateRef,
  ref,
  onMounted,
  onUpdated,
  onBeforeUnmount,
  watch,
} from "vue";

import ChartContext from "../ChartContext.vue";
import ChartLegend from "../ChartLegend/ChartLegend.vue";

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

import type { PieCharts, ColorPalette } from "../../../../types/viz";

type PieDataEntry = [name: string, d: number];

const props = withDefaults(defineProps<PieCharts>(), {
  width: 300,
  height: 300,
  margins: 10,
  showValues: true,
  showLabels: true,
  showValuesAsPercentages: true,
  asDonutChart: false,
  pieChartIsCentered: false,
  hoverEventsAreEnabled: false,
  clickEventsAreEnabled: false,
  legendIsEnabled: true,
  legendIsStacked: false,
  legendPosition: "top",
  legendClickEventsAreEnabled: false,
  legendHoverEventsAreEnabled: false,
});

const emits = defineEmits(["slice-clicked"]);

const parentElem = ref<HTMLElement>();
const container = useTemplateRef("container");

const svg = ref();
const chartArea = ref();
const chartScale = ref<number>(1);
const width = ref<number>(props.width);
const viewBox = ref<string>("");
const chartAreaTransform = ref<string>("");

const chartData = ref();
const arcGenerator = ref();
const labelGenerator = ref();
const radius = ref<number>(1);

function setChartDimensions() {
  parentElem.value = container.value?.parentNode as HTMLElement;
  width.value = parentElem.value.offsetWidth || props.width;

  viewBox.value = `0 0 ${width.value} ${props.height}`;
  chartAreaTransform.value = `translate(${(width.value - props.margins) / 2}, ${
    (props.height - props.margins) / 2
  })`;

  radius.value = Math.min(width.value, props.height) / 2 - props.margins;
}

function createChartGenerator() {
  const p = d3
    .pie<PieDataEntry>()
    .sort(null)
    .value((d) => {
      return d[1];
    });
  return props.asDonutChart ? p.padAngle(1 / radius.value) : p;
}

function createArcGenerator() {
  const arc = d3.arc().outerRadius(radius.value * 0.7);
  return props.asDonutChart
    ? arc.innerRadius(radius.value * 0.4)
    : arc.innerRadius(0);
}

function createLabelGenerator() {
  const r = props.asDonutChart ? radius.value - 1 : radius.value * 0.8;
  return d3.arc().innerRadius(r).outerRadius(r);
}

function setLabelPosition(value: Record<string, any>) {
  const position = labelGenerator.value.centroid(value);
  const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
  position[0] = radius.value * 0.99 * (angle < Math.PI ? 1 : -1);
  return position;
}

function setTextAnchor(value: Record<string, any>): string {
  const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
  return angle < Math.PI ? "start" : "end";
}

function setOffsetX(value: Record<string, any>): string {
  const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
  return angle < Math.PI ? "0.25em" : "-0.25em";
}

function setLabelText(value: string): string {
  return props.showValuesAsPercentages ? `${value}%` : value;
}

function onMouseOver(value: Record<string, any>) {
  const slice = chartArea.value.select(`.slice[data-group="${value}"]`);
  slice.node().classList.add("slice-focused");
}

function onMouseOut(value: Record<string, any>) {
  const slice = chartArea.value.select(`path.slice[data-group="${value}"]`);
  slice.node().classList.remove("slice-focused");
}

function onClick(value: Record<string, any>) {
  const data: Record<string, number> = {};
  data[value.data[0]] = value.data[1];
  // When segement is clicked, the underlying data (category and value) is returned
  emits("slice-clicked", data);
}

const colorPalette = computed<ColorPalette>(() => {
  if (!props.colorPalette) {
    const autoColorPalette: Record<string, string> = {};
    const domain = Object.keys(props.data);
    const length = domain.length === 2 ? 3 : Object.keys(props.data).length;

    const palette = d3.scaleOrdinal(d3.schemeBlues[length]);
    Object.keys(props.data)
      .sort()
      .forEach((key: string, index: number) => {
        autoColorPalette[key] = palette(index as unknown as string);
      });
    return autoColorPalette;
  }
  return props.colorPalette;
});

function drawSlices() {
  const pieSlices = svg.value.select(".pie-slices");
  pieSlices.selectAll("*").remove();

  const slices = pieSlices
    .selectAll("slices")
    .data(chartData.value)
    .join("path")
    .attr("d", arcGenerator.value)
    .attr("data-group", (value: Record<string, any>) => value.data[0])
    .attr(
      "fill",
      (value: Record<string, any>) => colorPalette.value[value.data[0]]
    );

  if (props.strokeColor) {
    slices.attr("stroke", props.strokeColor);
  } else {
    slices.attr("class", "stroke-chart-paths");
  }

  if (props.clickEventsAreEnabled) {
    slices
      .style("cursor", "pointer")
      .on("click", (_: Event, value: Record<string, any>) => onClick(value));
  }
}

function drawLabels() {
  const sliceLabels = svg.value.select("g.pie-slice-labels");
  sliceLabels.selectAll("*").remove();

  sliceLabels
    .selectAll("slice-label-path")
    .data(chartData.value)
    .join("polyline")
    .attr("fill", "none")
    .attr("stroke", props.strokeColor)
    .attr("class", "stroke-chart-paths")
    .attr("data-group", (value: Record<string, any>) => value.data[0])
    .attr("points", (value: Record<string, any>) => {
      const centroid = arcGenerator.value.centroid(value);
      const outerCircleCentroid = labelGenerator.value.centroid(value);
      const labelPosition = labelGenerator.value.centroid(value);
      const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
      labelPosition[0] = radius.value * 0.95 * (angle < Math.PI ? 1 : -1);
      return [centroid, outerCircleCentroid, labelPosition];
    });

  const labels = sliceLabels
    .selectAll("slice-label-text")
    .data(chartData.value)
    .join("text")
    .attr("class", "fill-chart-text")
    .style("font-size", "16px")
    .attr("data-group", (value: Record<string, any>) => value.data[0])
    .attr("y", (value: Record<string, any>) => setLabelPosition(value)[1])
    .style("text-anchor", setTextAnchor);

  if (props.showValues && props.showLabels) {
    labels
      .append("tspan")
      .attr("x", (value: Record<string, any>) => setLabelPosition(value)[0])
      .attr("dy", "0")
      .attr("dx", (value: Record<string, any>) => setOffsetX(value))
      .text((value: Record<string, any>) => value.data[0]);

    labels
      .append("tspan")
      .attr("dy", "1.1em")
      .attr("x", (value: Record<string, any>) => setLabelPosition(value)[0])
      .attr("dx", (value: Record<string, any>) => setOffsetX(value))
      .text((value: Record<string, any>) => setLabelText(value.data[1]));
  } else {
    labels
      .attr("x", (value: Record<string, any>) => setLabelPosition(value)[0])
      .attr("dx", (value: Record<string, any>) => setOffsetX(value))
      .attr("dy", "0.25em")
      .text((value: Record<string, any>) => {
        if (props.showLabels && !props.showValues) {
          return value.data[0];
        } else {
          return setLabelText(value.data[1]);
        }
      });
  }
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");
  setChartDimensions();

  const pieChart = createChartGenerator();
  arcGenerator.value = createArcGenerator();
  labelGenerator.value = createLabelGenerator();

  chartData.value = pieChart(Object.entries(props.data));

  drawSlices();

  if (props.showValues || props.showLabels) {
    drawLabels();
  }
}

onMounted(() => {
  renderChart();
  window.addEventListener("resize", renderChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", renderChart);
});

watch(props, () => renderChart(), { deep: true });
</script>

<template>
  <div ref="container">
    <ChartContext :title="title" :description="description" />
    <ChartLegend
      v-if="legendIsEnabled"
      :legend-id="id"
      :data="colorPalette"
      :stack-legend="legendIsStacked"
      class="mt-2.5"
    />
    <div :class="{ 'm-auto': pieChartIsCentered }">
      <svg
        :id="id"
        width="100%"
        :height="height"
        preserve-aspect-ratio="xMinYMin"
        :view-box="viewBox"
      >
        <g class="chart-area" :transform="chartAreaTransform">
          <g class="pie-slices"></g>
          <g class="pie-slice-labels"></g>
        </g>
      </svg>
    </div>
  </div>
</template>
