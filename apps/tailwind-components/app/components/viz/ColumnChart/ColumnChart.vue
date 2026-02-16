<script lang="ts" setup>
import { ref, computed, useTemplateRef, onMounted, watch } from "vue";
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

import ChartContext from '../ChartContext.vue';
import ChartLegend from '../ChartLegend/ChartLegend.vue';

import type { ColumnCharts, ColorPalette } from "../../../../types/viz";
import { set_chart_legend_layout_css } from "../../../utils/viz";

const props = withDefaults(defineProps<ColumnCharts>(),{
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
    legendIsEnabled: true,
    legendIsStacked: false,
    legendPosition: "top",
    legendHoverEventsAreEnabled: false,
});

const emits = defineEmits(["column-clicked"]);

const parentElem = ref<HTMLElement>();
const container = useTemplateRef("container");

const svg = ref();
const chartArea = ref();
const width = ref<number>(props.width);
const height = ref<number>(props.height);
const viewBox = ref<string>("");
const chartAreaTransform = ref<string>("");
    
// define values that might become props later
const columnPaddingInner: number = 0.2;
const columnPaddingOuter: number = 0.2;
const columnAlign: number = 0.5;

const chartLayoutCss = computed<string>(() => {
    return set_chart_legend_layout_css(props.legendIsEnabled, props.legendPosition)
});

    
function setChartDimensions() {
  parentElem.value = container.value?.parentNode as HTMLElement;
  width.value = (parentElem.value?.offsetWidth || props.width) - props.marginLeft - props.marginRight;
  height.value = props.height - props.marginTop - props.marginBottom;

  viewBox.value = `0 0 ${width.value} ${height.value}`;
  chartAreaTransform.value = `translate(${width.value / 2},${
    height.value / 2
  })`;

}

const colorPalette = computed<ColorPalette>(() => {
    
    // TODO: refactor this
    
//   if (!props.colorPalette) {
    // const autoColorPalette: Record<string, string> = {};
    // const domain = Object.keys(props.data);
    // const length = domain.length === 2 ? 3 : Object.keys(props.data).length;

    // const palette = d3.scaleOrdinal(d3.schemeBlues[length]);
    // Object.keys(props.data)
    //   .sort()
    //   .forEach((key: string, index: number) => {
        // autoColorPalette[key] = palette(index as unknown as string);
    //   });
    // return autoColorPalette;
//   }
  return props.colorPalette as ColorPalette;
});

function renderChart() {
    svg.value = d3.select(`#${props.id}`);
    chartArea.value = svg.value.select("g.chart-area");
    setChartDimensions();
}

onMounted(() => {
})

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
                'm-auto': legendIsEnabled && legendPosition === 'bottom'
            }"
        />
        <div style="grid-area: chart;">
            <svg
                :id="id"
                width="100%"
                :height="height"
                preserve-aspect-ratio="xMinYMin"
                :view-box="viewBox"
            >
                <g class="chart-area" :transform="`translate(${margin_left}, ${margin_top})`">
                    <g class="chart-axes">
                        <g class="chart-axis-x" :transform="`translate(0,${height})`"></g>
                        <g class="chart-axis-y"></g>
                    </g>
                </g>
                <g class="chart-columns">
                    
                </g>
            </svg>
        </div>
    </div>
</template>