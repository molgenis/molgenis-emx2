<template>
  <ul
    class="list-style-none flex m-0 p-0"
    :class="{
      'flex-col gap-0': stackLegend,
      'flex-row flex-wrap justify-start gap-3': !stackLegend,
      'legend-clicked-enabled': enableClicks,
      'legend-hovering-enabled': enableHovering,
    }"
  >
    <li
      class="relative cursor-default"
      :class="{
        'mb-2': !stackLegend,
        'hover:cursor-pointer': enableHovering || enableClicks,
      }"
      data-legend-item="item"
      v-for="key in Object.keys(data)"
      :key="key"
    >
      <div
        v-if="enableClicks"
        class="flex flex-row gap-3 justify-start items-center"
      >
        <label
          :for="`legend-input-${legendId}-${key}`"
          class="flex flex-row gap-3 justify-start items-center hover:underline hover:cursor-pointer"
        >
          <ChartLegendMarker :markerType="markerType" :fill="data[key]" />
          <input
            :id="`legend-input-${legendId}-${key}`"
            class="sr-only [&:focus~span]:underline"
            :class="{
              '[&:checked~span]:line-through': enableMultiSelect,
              '[&~span]:hover:underline': !enableMultiSelect,
              '[&~span]:line-through':
                legendSelections.length &&
                !enableMultiSelect &&
                !legendSelections.includes(key),
            }"
            :type="enableMultiSelect ? 'checkbox' : 'radio'"
            :name="`legend-name-${legendId}`"
            :value="key"
            v-model="legendSelections"
            @change="$emit('legend-item-clicked', legendSelections)"
          />
          <span class="text-current">{{ key }}</span>
        </label>
      </div>
      <div
        v-else
        class="flex flex-row gap-3 justify-start items-center"
        :class="{
          'hover:underline': enableHovering,
        }"
        :data-value="key"
        @mouseover="$emit('legend-item-mouseover', key)"
        @mouseout="$emit('legend-item-mouseout', key)"
      >
        <ChartLegendMarker :markerType="markerType" :fill="data[key]" />
        <span>{{ key }}</span>
      </div>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ref } from "vue";

interface ILegendProps {
  legendId: string;
  data: Record<string, any>;
  stackLegend?: boolean;
  enableClicks?: boolean;
  enableHovering?: boolean;
  enableMultiSelect?: boolean;
  markerType?: "circle" | "square";
}

withDefaults(defineProps<ILegendProps>(), {
  stackLegend: false,
  enableClicks: false,
  enableHovering: false,
  enableMultiSelect: true,
  markerType: "circle",
});

const emit = defineEmits<{
  (e: "legend-item-clicked", value: string[]): void;
  (e: "legend-item-mouseover", value: string): void;
  (e: "legend-item-mouseout", value: string): void;
}>();

const legendSelections = ref<string[]>([]);
</script>
