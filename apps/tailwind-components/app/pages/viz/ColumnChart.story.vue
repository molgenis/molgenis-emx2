<script lang="ts" setup>
import { ref } from "vue";
import ColumnChart from "../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentOutput from "../../components/story/ComponentOutput.vue";
import InputBoolean from "../../components/input/Boolean.vue";

const enableClicks = ref<boolean>(true);
const enableHover = ref<boolean>(true);

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 3 },
];

const clickedElem = ref<Record<string, number>>();
</script>

<template>
  <div class="h-auto grid gap-2.5 md:grid-cols-3">
    <div class="col-span-2">
      <ColumnChart
        id="column-chart-demo"
        title="Participants by experimental group"
        description="Participants (n=158) were randomised into one of four groups"
        :data="data"
        xvar="name"
        yvar="value"
        x-axis-label="Experimental group"
        y-axis-label="Number of participants"
        :click-events-are-enabled="enableClicks"
        :hover-events-are-enabled="enableHover"
        @column-clicked="clickedElem = $event"
      />
      <ComponentOutput class="mt-2.5" v-if="enableClicks">
        Clicked element: {{ clickedElem }}
      </ComponentOutput>
    </div>
    <div>
      <form class="grid grid-cols-1 gap-5">
        <legend class="text-title text-heading-2xl">Configure chart</legend>
        <div>
          <label for="column-chart-enable-clicks" class="text-title">
            Enable click events?
          </label>
          <InputBoolean
            id="column-chart-enable-clicks"
            v-model="enableClicks"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
        <div>
          <label for="column-chart-enable-hover" class="text-title">
            Enable hover events?
          </label>
          <InputBoolean
            id="column-chart-enable-hover"
            v-model="enableHover"
            :show-clear-button="false"
            align="horizontal"
          />
        </div>
      </form>
    </div>
  </div>
</template>
