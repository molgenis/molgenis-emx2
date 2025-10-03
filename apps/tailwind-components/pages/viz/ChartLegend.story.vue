<script setup lang="ts">
import { ref } from "vue";

const stackLegend = ref<boolean>(false);
const legendMarkerShape = ref<"circle" | "square">("circle");
const legendEnableMultiselect = ref<boolean>(true);
const legendItemHovered = ref<string>();
const legendItemClicked = ref<string | string[]>();

const legendEventsOptions = [
  { value: "hover", label: "Hovering" },
  { value: "click", label: "Clicking" },
];
const legendEnableEvent = ref<string>(legendEventsOptions[0]?.value ?? "hover");

const legendData: Record<string, string> = {
  "Group A": "#f6eff7",
  "Group B": "#bdc9e1",
  "Group C": "#67a9cf",
  "Group D": "#02818a",
};
</script>

<template>
  <div class="grid grid-cols-2 gap-2 mt-4">
    <div>
      <ChartLegend
        legend-id="story-legend"
        class="text-input mb-4"
        :data="legendData"
        :stack-legend="stackLegend"
        :marker-type="legendMarkerShape"
        :enable-hovering="legendEnableEvent === 'hover'"
        :enable-clicks="legendEnableEvent === 'click'"
        :enable-multi-select="legendEnableMultiselect"
        @legend-item-mouseover="legendItemHovered = $event"
        @legend-item-clicked="legendItemClicked = $event"
      />
      <StoryComponentOutput class="mt-2">
        <code v-if="legendEnableEvent === 'hover'">
          Hovered item: {{ legendItemHovered }}
        </code>
        <code v-else> Clicked items: {{ legendItemClicked }} </code>
      </StoryComponentOutput>
    </div>
    <form class="grid grid-cols-1 gap-4 [&_label]:mr-4">
      <legend class="text-title text-heading-2xl">Input props</legend>
      <div>
        <label for="legend-stack" class="text-title">
          Stack legend the legend?
        </label>
        <InputBoolean
          id="legend-stack"
          v-model="stackLegend"
          :show-clear-button="false"
          align="horizontal"
        />
      </div>
      <div>
        <label for="legend-marker-type" class="text-title">
          Change the legend item shape
        </label>
        <InputRadioGroup
          id="legend-marker-type"
          :options="[{ value: 'circle' }, { value: 'square' }]"
          v-model="legendMarkerShape"
          :show-clear-button="false"
          align="horizontal"
        />
      </div>
      <div>
        <label for="legend-events-enable" class="text-title">
          Choose event type
        </label>
        <InputRadioGroup
          id="legend-events-enable"
          :options="legendEventsOptions"
          v-model="legendEnableEvent"
          :show-clear-button="false"
          align="horizontal"
        />
      </div>
      <div v-if="legendEnableEvent === 'click'">
        <label for="legend-events-enable-multiselect" class="text-title">
          Enable multiselect?
        </label>
        <InputBoolean
          id="legend-events-enable-multiselect"
          v-model="legendEnableMultiselect"
          :show-clear-button="false"
          align="horizontal"
        />
      </div>
    </form>
  </div>
</template>
