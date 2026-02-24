<script setup lang="ts">
import { useEventListener } from "@vueuse/core";
import { computed, useTemplateRef, ref, onMounted, watch } from "vue";

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

import { setChartLegendLayoutCss } from "../../../utils/viz";
import type { PieCharts, ColorPalette } from "../../../../types/viz";

type PieDataEntry = [name: string, d: number];

const props = withDefaults(defineProps<PieCharts>(), {
  width: 250,
  height: 250,
  margins: 25,
  showValues: true,
  showLabels: true,
  showValuesAsPercentages: true,
  asDonutChart: false,
  hoverEventsAreEnabled: true,
  clickEventsAreEnabled: false,
  legendIsEnabled: true,
  legendIsStacked: false,
  legendPosition: "top",
  legendHoverEventsAreEnabled: false,
});

const emits = defineEmits(["slice-clicked"]);

const parentElem = ref<HTMLElement>();
const container = useTemplateRef("container");

const svg = ref();
const chartArea = ref();
const width = ref<number>(props.width);
const height = ref<number>(props.height);
const viewBox = ref<string>("");
const chartAreaTransform = ref<string>("");

const chartData = ref();
const arcGenerator = ref();
const labelGenerator = ref();
const radius = ref<number>(1);

const chartLayoutCss = computed<string>(() => {
  return setChartLegendLayoutCss(props.legendIsEnabled, props.legendPosition);
});

function setChartDimensions() {
  parentElem.value = container.value?.parentNode as HTMLElement;
  width.value = (parentElem.value?.offsetWidth || props.width) - props.margins;
  height.value = props.height - props.margins;

  viewBox.value = `0 0 ${width.value} ${height.value}`;
  chartAreaTransform.value = `translate(${width.value / 2},${
    height.value / 2
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

function onMouseOver(value: string) {
  const elem = chartArea.value.select(
    `path[data-elem="slice"][data-group="${value}"]`
  );
  elem.node().classList.add("scale-125");
}

function onMouseOut(value: string) {
  const elem = chartArea.value.select(
    `path[data-elem="slice"][data-group="${value}"]`
  );
  elem.node().classList.remove("scale-125");
}

function onClick(value: Record<string, any>) {
  const data: Record<string, number> = {};
  data[value.data[0]] = value.data[1];
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
    .attr("data-elem", "slice")
    .attr("data-group", (value: Record<string, any>) => value.data[0])
    .attr(
      "fill",
      (value: Record<string, any>) => colorPalette.value[value.data[0]]
    );

  const sliceCss: string[] = ["duration-300", "ease-in-out"];
  if (props.strokeColor) {
    slices.attr("stroke", props.strokeColor);
  } else {
    sliceCss.push("stroke-chart-paths");
  }

  if (props.clickEventsAreEnabled || props.hoverEventsAreEnabled) {
    sliceCss.push("cursor-pointer");
  }

  slices.attr("class", sliceCss.join(" "));

  if (props.clickEventsAreEnabled) {
    slices.on("click", (_: Event, value: Record<string, any>) =>
      onClick(value)
    );
  }

  if (props.hoverEventsAreEnabled) {
    slices
      .on("mouseover", (_: Event, value: Record<string, any>) => {
        onMouseOver(value.data[0]);
      })
      .on("mouseout", (_: Event, value: Record<string, any>) => {
        onMouseOut(value.data[0]);
      });
  }
}

function drawLabels() {
  const sliceLabels = svg.value.select("g.pie-slice-labels");
  sliceLabels.selectAll("*").remove();

  sliceLabels
    .selectAll("polylines")
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
    const text = labels
      .attr("x", (value: Record<string, any>) => setLabelPosition(value)[0])
      .attr("dx", (value: Record<string, any>) => setOffsetX(value))
      .attr("dy", "0.25em");
    if (props.showLabels && !props.showValues) {
      text.text((value: Record<string, any>) => value.data[0]);
    } else {
      text.text((value: Record<string, any>) => setLabelText(value.data[1]));
    }
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
  } else {
    const sliceLabels = svg.value.select("g.pie-slice-labels");
    sliceLabels.selectAll("*").remove();
  }
}

onMounted(() => {
  renderChart();
  useEventListener("resize", renderChart);
});

watch(props, () => renderChart(), { deep: true });
</script>

<template>
  <div ref="container" class="grid gap-2.5 w-full" :class="[chartLayoutCss]">
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
      @legend-item-mouseover="(value:string) => onMouseOver(value)"
      @legend-item-mouseout="(value:string) => onMouseOut(value)"
      class="mb-2.5"
      :class="{
        'm-auto': legendIsEnabled && legendPosition === 'bottom',
      }"
      style="grid-area: legend"
    />
    <div style="grid-area: chart">
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
