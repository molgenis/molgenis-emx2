<script setup lang="ts">
import { useEventListener } from "@vueuse/core";
import { computed, useTemplateRef, ref, onMounted, watch } from "vue";

import ChartTitle from "../ChartTitle.vue";
import ChartLegend from "../ChartLegend/ChartLegend.vue";

import {
  select,
  selectAll,
  scaleOrdinal,
  pie,
  arc,
  schemeBlues,
  sort,
  interpolate,
} from "d3";
const d3 = {
  select,
  selectAll,
  scaleOrdinal,
  pie,
  arc,
  schemeBlues,
  sort,
  interpolate,
};

import { setChartLegendLayoutCss } from "../../../utils/viz";
import type { PieCharts, ColorPalette } from "../../../../types/viz";

type PieDataEntry = [name: string, d: number];
interface ArcItemData {
  startAngle: number;
  endAngle: number;
  innerRadius: number;
  outerRadius: number;
}

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

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // defined by d3.select
const chartArea = ref(); // defined by d3.select

const width = ref<number>(0);
const height = computed<number>(() => {
  return props.height - props.margins;
});

const chartData = ref();

const arcGenerator = computed(() => {
  const arc = d3.arc().outerRadius(radius.value * 0.7);
  return props.asDonutChart
    ? arc.innerRadius(radius.value * 0.375)
    : arc.innerRadius(0);
});

const labelGenerator = computed(() => {
  const r = props.asDonutChart ? radius.value - 1 : radius.value * 0.8;
  return d3.arc().innerRadius(r).outerRadius(r);
});

const radius = computed<number>(() => {
  return Math.min(width.value, props.height) / 2 - props.margins;
});

function createChartGenerator() {
  const p = d3
    .pie<PieDataEntry>()
    .sort(null)
    .value((d) => {
      return d[1];
    });
  return props.asDonutChart ? p.padAngle(1 / radius.value) : p;
}

function setLabelPosition(value: ArcItemData) {
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
  const elem = chartArea.value.select(`g[data-group="${value}"] path`);
  elem.node().classList.add("scale-125");
}

function onMouseOut(value: string) {
  const elem = chartArea.value.select(`g[data-group="${value}"] path`);
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

function addSliceEvents() {
  const slices = chartArea.value
    .selectAll("path.pie-segments")
    .data(chartData.value);

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

function calculatePolylinePoints(value: ArcItemData): string {
  const centroid = arcGenerator.value.centroid(value);
  const outerCircleCentroid = labelGenerator.value.centroid(value);
  const labelPosition = labelGenerator.value.centroid(value);
  const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
  labelPosition[0] = radius.value * 0.95 * (angle < Math.PI ? 1 : -1);
  return `${centroid},${outerCircleCentroid},${labelPosition}`;
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");
  width.value = (parentElem.value?.offsetWidth || props.width) - props.margins;

  const pieChart = createChartGenerator();
  chartData.value = pieChart(Object.entries(props.data));

  addSliceEvents();
}

onMounted(() => {
  renderChart();
  useEventListener("resize", renderChart);
});

watch(
  () => [props.data],
  () => renderChart(),
  { deep: true }
);
</script>

<template>
  <div
    ref="container"
    class="grid gap-2.5 w-full"
    :class="
      setChartLegendLayoutCss(props.legendIsEnabled, props.legendPosition)
    "
  >
    <ChartTitle
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
        :width="width"
        :height="height"
        preserve-aspect-ratio="xMinYMin"
        :viewBox="`0 0 ${width} ${height}`"
      >
        <g
          class="chart-area"
          :transform="`translate(${width / 2}, ${height / 2})`"
        >
          <g
            v-for="row in chartData"
            class="pie-group"
            :data-group="row.data[0]"
            :data-value="row.data[1]"
          >
            <path
              :d="(arcGenerator(row) as string)"
              :fill="colorPalette[row.data[0]]"
              class="pie-segments"
              :class="{
                'stroke-chart-paths': !strokeColor,
                'cursor-pointer duration-300 ease-in-out':
                  clickEventsAreEnabled || hoverEventsAreEnabled,
              }"
              :stroke="strokeColor ? strokeColor : undefined"
            />
            <polyline
              v-if="showLabels || showValues"
              fill="none"
              :class="{
                'stroke-chart-paths': !strokeColor,
                'cursor-pointer':
                  clickEventsAreEnabled || hoverEventsAreEnabled,
              }"
              :stroke="strokeColor ? strokeColor : undefined"
              :points="calculatePolylinePoints(row)"
            />
            <text
              v-if="showLabels && showValues"
              class="fill-chart-text"
              :y="setLabelPosition(row)[1]"
              :text-anchor="setTextAnchor(row)"
            >
              <tspan :x="setLabelPosition(row)[0]" :dx="setOffsetX(row)" dy="0">
                {{ row.data[0] }}
              </tspan>
              <tspan
                :x="setLabelPosition(row)[0]"
                :dx="setOffsetX(row)"
                dy="1.1em"
              >
                {{ setLabelText(row.data[1]) }}
              </tspan>
            </text>
            <text
              v-else-if="showLabels || showValues"
              class="fill-chart-text"
              :x="setLabelPosition(row)[0]"
              :y="setLabelPosition(row)[1]"
              :dx="setOffsetX(row)"
              dy="0.25em"
              :text-anchor="setTextAnchor(row)"
            >
              {{
                showLabels && !showValues
                  ? row.data[0]
                  : setLabelText(row.data[1])
              }}
            </text>
          </g>
        </g>
      </svg>
    </div>
  </div>
</template>
