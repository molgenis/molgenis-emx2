<template>
  <fieldset class="chart-selector">
    <legend>Select a chart type</legend>
    <div class="chart-type-input" title="Bar Chart">
      <input
        :id="`${id}-bar`"
        type="radio"
        :name="`${id}-chart-type`"
        value="barChart"
        v-model="chartType"
        @change="onChange"
      />
      <label :for="`${id}-bar`">
        <BarChartIcon class="icons bar-chart" />
        <span class="visually-hidden">bar chart</span>
      </label>
    </div>
    <div class="chart-type-input" title="Column Chart">
      <input
        :id="`${id}-column`"
        type="radio"
        :name="`${id}-chart-type`"
        value="columnChart"
        v-model="chartType"
        @change="onChange"
      />
      <label :for="`${id}-column`">
        <columnChartIcon class="icons column-chart" />
        <span class="visually-hidden">column chart</span>
      </label>
    </div>
    <div class="chart-type-input" title="Pie Chart">
      <input
        :id="`${id}-pie`"
        type="radio"
        :name="`${id}-chart-type`"
        v-model="chartType"
        @change="onChange"
        value="pieChart"
      />
      <label :for="`${id}-pie`">
        <span class="visually-hidden">pie chart</span>
        <PieChartIcon class="icons pie-chart" />
      </label>
    </div>
    <div class="chart-type-input" title="Scatter Plot">
      <input
        :id="`${id}-scatter-plot`"
        type="radio"
        :name="`${id}-chart-type`"
        v-model="chartType"
        @change="onChange"
        value="scatterPlot"
      />
      <label :for="`${id}-scatter-plot`">
        <span class="visually-hidden">scatter plot</span>
        <ScatterPlotIcon class="icons scatter-plot" />
      </label>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import { ref } from "vue";
import BarChartIcon from "../components/icons/viz-bar-chart.vue";
import columnChartIcon from "../components/icons/viz-column-chart.vue";
import PieChartIcon from "../components/icons/viz-pie-chart.vue";
import ScatterPlotIcon from "../components/icons/viz-scatter-plot.vue";

interface Props {
  id: String;
}

defineProps<Props>();

const chartType = defineModel<String>("barChart");
const emit = defineEmits<{
  change: [value: string];
}>();

function onChange(event: Event) {
  emit("change", (event.target as HTMLInputElement).value);
}
</script>

<style lang="scss">
.chart-selector {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  width: 100%;
  gap: 1em;

  legend {
    display: block;
    color: $gray-900;
    font-size: 1.4rem;
  }

  .chart-type-input {
    flex-grow: 1;

    input[type="radio"] {
      @include visuallyHidden;

      &:checked {
        & + label {
          background-color: $blue-800;

          svg {
            rect,
            circle {
              fill: $blue-050;
            }
            path {
              stroke: $blue-050;
            }

            &.pie-chart {
              path {
                fill: $blue-050;
                stroke: none;
              }
            }
          }
        }
      }
    }

    label {
      display: flex;
      justify-content: center;
      align-items: center;
      box-sizing: content-box;
      width: 100%;
      background-color: $gray-050;
      padding: 0.4em 0;
      margin: 0;
      gap: 1em;
      cursor: pointer;

      .icons {
        $size: 28px;
        width: $size;
        height: $size;
      }
    }
  }
}
</style>
