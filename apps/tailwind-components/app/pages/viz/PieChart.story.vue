<script lang="ts" setup>
import { ref } from "vue";

import PieChart from "../../components/viz/PieChart/PieChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";
import InputBoolean from "../../components/input/Boolean.vue";
import type { LegendPosition } from "../../../types/viz";

const enableLegend = ref<boolean>(true);
const chartPosition = ref<LegendPosition>("top");
const asDonutChart = ref<boolean>(true);
const showValues = ref<boolean>(true);
const showLabels = ref<boolean>(true);
const enableClicks = ref<boolean>(false);
const enableHover = ref<boolean>(false);

const data = {
  "Group A": 48,
  "Group B": 32,
  "Group C": 11,
  Other: 9,
};

const clickedSlice = ref<Record<string, number>>();
</script>
<template>
  <div class="h-auto grid gap-2.5 md:grid-cols-3">
    <div class="col-span-2">
      <PieChart
        id="pie-chart-demo"
        title="Recruitment overview"
        description="Group assignment after randomization (n=100)"
        :data="data"
        :as-donut-chart="asDonutChart"
        :click-events-are-enabled="enableClicks"
        :hover-events-are-enabled="enableHover"
        :legend-is-enabled="enableLegend"
        :legend-position="chartPosition"
        :show-values="showValues"
        :show-labels="showLabels"
        @slice-clicked="clickedSlice = $event"
      />
      <ComponentOutput class="mt-2.5" v-if="enableClicks">
        Clicked segment: {{ clickedSlice }}
      </ComponentOutput>
    </div>
    <div>
      <form class="grid grid-cols-1 gap-5">
        <legend class="text-title text-heading-2xl">Configure chart</legend>
        <div>
          <label for="pie-chart-enable-legend" class="text-title"
            >Enable chart legend?</label
          >
          <InputBoolean
            id="pie-chart-enable-legend"
            v-model="enableLegend"
            align="horizontal"
            :show-clear-button="false"
          />
        </div>
        <div v-if="enableLegend">
          <label for="pie-chart-legend-position" class="text-title">
            Change the position of the legend
          </label>
          <InputRadioGroup
            id="pie-chart-legend-position"
            :options="[{ value: 'top' }, { value: 'bottom' }]"
            v-model="chartPosition"
            align="horizontal"
          />
        </div>
        <div>
          <label for="pie-chart-as-donut" class="text-title">
            Render as donut chart?
          </label>
          <InputBoolean
            id="pie-chart-as-donut"
            v-model="asDonutChart"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
        <div>
          <label for="pie-chart-show-labels" class="text-title">
            Show labels?
          </label>
          <InputBoolean
            id="pie-chart-show-labels"
            v-model="showLabels"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
        <div>
          <label for="pie-chart-show-values" class="text-title">
            Show values?
          </label>
          <InputBoolean
            id="pie-chart-show-values"
            v-model="showValues"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
        <div>
          <label for="pie-chart-enable-clicks" class="text-title">
            Enable click events?
          </label>
          <InputBoolean
            id="pie-chart-enable-clicks"
            v-model="enableClicks"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
        <div>
          <label for="pie-chart-enable-hover" class="text-title">
            Enable hover events?
          </label>
          <InputBoolean
            id="pie-chart-enable-hover"
            v-model="enableHover"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
      </form>
    </div>
  </div>
</template>
