<script lang="ts" setup>
import { ref, computed, useTemplateRef } from "vue";

import { select } from "d3";
const d3 = { select };

import ChartTitle from '../ChartTitle.vue';

import { newNumericAxisGenerator, newCategoricalAxisGenerator } from "../../../utils/viz";

import type { ProgressMeter } from '../../../../types/viz';

const props = withDefaults(
    defineProps<ProgressMeter>(),
        {
            color: "#0173e4",
            width: 500,
            height: 15,
            marginTop: 0,
            marginRight: 10,
            marginBottom: 0,
            marginLeft: 10,
            animationsAreEnabled: true,
        }
)

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});

const svg = ref(); // receives d3.select
const chartArea = ref(); // receives d3.select

const width = ref<number>(0);

const height = computed<number>(() => {
  return props.height - props.marginTop - props.marginBottom;
});

const xScale = computed(() => {
   return newCategoricalAxisGenerator({
    domains: [props.label],
    rangeEnd: width.value
   }) 
});

function renderChart() {
    svg.value = d3.select(`#${props.id}`);
  chartArea.value = svg.value.select("g.chart-area");
  
  width.value = (parentElem.value?.offsetWidth || props.width) - props.marginLeft - props.marginRight;
}

</script>

<template>
    <div ref="container" class="grid gap-2.5 w-ful chart_layout_default">
        <ChartTitle
            :title="title"
            :description="description"
            style="grid-area: context;"
        />
        <div style="grid-area: chart;">
            <svg
                width="100%"
                height="100%"
                :viewBox="`0 0 ${width + marginLeft + marginRight} ${height}`"
            >
                <g class="chart-area">
                    <rect
                        fill="none"
                        :stroke="color"
                        stroke-width="2"
                    />
                    <rect
                        class="bar"
                        :fill="color"
                        :stroke="color"
                        stroke-width="2"
                    />
                </g>
            </svg>
        </div>
    </div>
</template>