<script setup lang="ts">
withDefaults(
  defineProps<{
    legendId: string;
    data: Record<string, any>;
    stackLegend?: boolean;
    enableClicks?: boolean;
    enableHovering?: boolean;
    enableMultiSelect?: boolean;
    markerType?: "circle" | "square";
  }>(),
  {
    stackLegend: false,
    enableClicks: false,
    enableHovering: false,
    enableMultiSelect: false,
    markerType: "circle",
  }
);

const emit = defineEmits<{
  (e: "legend-item-clicked", value: string[]): void;
  (e: "legend-item-mouseover", value: string): void;
  (e: "legend-item-mouseout", value: string): void;
}>();

const legendSelections = ref<string[] | number[] | boolean[]>([]);
</script>

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
        <label :for="`legend-input-${legendId}-${key}`" class="flex flex-row gap-3 justify-start items-center hover:underline hover:cursor-pointer">
          <vizLegendMarker :markerType="markerType" :fill="data[key]" />
          <span class="text-body-base text-current">{{ key }}</span>
        </label>
        <input
          :id="`legend-input-${legendId}-${key}`"
          class="sr-only"
          type="checkbox"
          :name="`legend-name-${legendId}`"
          :value="key"
          v-model="legendSelections"
          @change="$emit('legend-item-clicked', legendSelections)"
        />
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
        <vizLegendMarker :markerType="markerType" :fill="data[key]" />
        <span class="item-label">
          {{ key }}
        </span>
      </div>
    </li>
  </ul>
</template>

<!-- <script>
// Create a legend for a visualisation for use outside the chart element. This
// component may be useful if you have several charts that display the
// same groups.
//
// @group VISUALISATIONS
export default {
  name: "ChartLegend",
  props: {
    // a unique ID for the legend
    legendId: {
      type: String,
      required: true,
    },

    // One or more key-value pairs
    data: {
      type: Object,
      required: true,
    },
    // If true (default), all legend items will be stacked
    stackLegend: {
      type: Boolean,
      default: true,
    },

    // If `true`, click events will be enabled for all labels. When a label is
    // clicked, the row-level data for that label will be emitted.
    // To access the data, use the event `@legend-item-clicked=(value) => ...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false,
    },

    // If `true`, mouseover event will be enabled for all labels. When a label is
    // clicked, the row-level data for that item will be emitted.
    // To access the data, use the event `legend-item-hovered=(value)=>...`
    enableHovering: {
      type: Boolean,
      // `false`
      default: false,
    },
  },
  emits: [
    "legend-item-clicked",
    "legend-item-mouseover",
    "legend-item-mouseout",
  ],
  data() {
    return {
      selection: [],
    };
  },
  methods: {
    emitSelection(event) {
      const parent = event.target.parentNode;
      parent.classList.toggle("checkbox-clicked");
      this.$emit("legend-item-clicked", this.selection);
    },
    emitMouseOver(value) {
      if (this.enableHovering) {
        this.$emit("legend-item-mouseover", value);
      }
    },
    emitMouseOut(value) {
      if (this.enableHovering) {
        this.$emit("legend-item-mouseout", value);
      }
    },
  },
  computed: {
    classNames() {
      const css = ["chart-legend"];
      if (!this.stackLegend) {
        css.push("legend-horizontal");
      }
      if (this.enableClicks) {
        css.push("clicks-enabled");
      }
      return css.join(" ");
    },
  },
};
</script>

<style lang="scss">
.chart-legend {
  list-style: none;
  padding: 0;
  margin: 0;

  .checkbox-item,
  .text-item {
    position: relative;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    flex-wrap: nowrap;
    column-gap: 4px;
    margin-bottom: 5px;

    &:last-child {
      margin: 0;
    }

    .item-marker {
      width: 16px;
      margin-right: 6px;
    }
  }

  .legend-item {
    cursor: default;

    &:hover {
      cursor: pointer;
    }
  }

  &.legend-horizontal {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 0.5em;

    .legend-item {
      margin-bottom: 0;
    }
  }

  &.clicks-enabled {
    .checkbox-item {
      .item-label {
        cursor: pointer;
      }
      &.checkbox-clicked {
        opacity: 0.65;
        .item-label {
          span {
            text-decoration: line-through;
          }
        }
      }
    }
  }
}
</style> -->
