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

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // receives d3.select
const chartArea = ref(); // receives d3.select

const width = ref<number>(0);

const height = computed<number>(() => {
  return props.height - props.marginTop - internalBottomMargin.value;
});

const internalLeftMargin = computed<number>(() => {
  return props.yAxisLabel ? props.marginLeft : 25;
});

const internalBottomMargin = computed<number>(() => {
  return props.xAxisLabel || props.breakXAxisLabelsAt ? props.marginBottom : 25;
});

const yAxisData = computed<NumericAxisTickData>(() => {
  const ticks: NumericAxisTickData = {
    ...generateAxisTickData(props.data, props.yvar),
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
  const data = { count: 0, domains: [] as string[] };
  if (props.data) {
    const values = props.data.map((row: DatasetRow) => row[props.xvar]);
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
  return newNumericAxisGenerator({
    domainLimit: yAxisData.value.limit,
    rangeStart: height.value,
  });
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

function renderChartAxes() {
  if (chartArea.value) {
    const xAxis = d3.axisBottom(xScale.value);
    const yAxis = d3.axisLeft(yScale.value).tickValues(yAxisData.value.ticks);
    const chartAxisGroup = chartArea.value.select("g.axes");
    chartAxisGroup.select(".x-axis").call(xAxis);
    chartAxisGroup.select(".y-axis").call(yAxis);
  }

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
  const columns = chartColumnArea.selectAll("rect").data(props.data);

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
}

function renderChart() {
  svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");

  width.value =
    (parentElem.value?.offsetWidth || props.width) -
    internalLeftMargin.value -
    props.marginRight;

  renderChartAxes();
  renderColumns();
}

onMounted(() => {
  renderChart();
  useEventListener("resize", renderChart);
});

watch(
  () => [props.data],
  () => {
    renderChart();
  },
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
        :width="width"
        :height="props.height"
        preserve-aspect-ratio="xMinYMin"
        :viewBox="`0 0 ${width + internalLeftMargin} ${props.height}`"
      >
        <g
          class="chart-area"
          :transform="`translate(${internalLeftMargin}, ${marginTop})`"
        >
          <g class="columns">
            <g
              v-for="row in data"
              class="rect-group"
              :key="row[xvar]"
              :data-x="row[xvar]"
              :data-y="row[yvar]"
            >
              <rect
                class="column"
                :fill="colorPalette[row[xvar]]"
                :width="xScale.bandwidth()"
                :x="xScale(row[xvar])"
                :stroke="columnBorderColor ? columnBorderColor : undefined"
                :stroke-width="columnBorderColor ? '1' : undefined"
              />
              <text
                class="fill-chart-text text-body-base"
                text-anchor="middle"
                :opacity="0"
                :x="xScale(row[xvar])"
                :y="yScale(row[yvar])"
                :dx="xScale.bandwidth() / 2"
                dy="-0.35em"
              >
                {{ row[yvar] }}
              </text>
            </g>
          </g>
          <g
            class="axes [&_text]:fill-chart-text [&_text]:text-body-sm [&_line]:stroke-chart-paths [&_path]:stroke-chart-paths"
          >
            <g class="x-axis" :transform="`translate(0,${height})`"></g>
            <g class="y-axis"></g>
          </g>
        </g>
        <g class="titles">
          <text
            v-if="xAxisLabel"
            class="fill-chart-text text-body-base"
            :x="width / 2"
            :y="
              yAxisData.min < 0
                ? yScale(yAxisData.min) + internalBottomMargin
                : yScale(0) + internalBottomMargin * 0.9
            "
            dy="0.5em"
          >
            {{ xAxisLabel }}
          </text>
          <text
            v-if="yAxisLabel"
            class="fill-chart-text -rotate-90 text-body-base"
            text-anchor="middle"
            :x="-height * 0.55"
            :y="internalLeftMargin / 2"
            dy="-0.8em"
          >
            {{ yAxisLabel }}
          </text>
        </g>
      </svg>
    </div>
  </div>
</template>
