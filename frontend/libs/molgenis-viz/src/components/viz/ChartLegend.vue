<template>
  <ul :class="classNames">
    <li
      class="legend-item"
      data-legend-item="item"
      v-for="key in Object.keys(data)"
      :key="key"
    >
      <div class="checkbox-item" v-if="enableClicks">
        <label :for="`cb-${key}`" class="item-label">
          <svg
            class="item-marker"
            width="16"
            height="16"
            viewBox="0 0 16 16"
            preserveXminYmin="true"
          >
            <circle cx="8" cy="8" r="8" :fill="data[key]" stroke-width="none" />
          </svg>
          <span>{{ key }}</span>
        </label>
        <input
          :id="`cb-${key}`"
          class="legend-checkbox visually-hidden"
          type="checkbox"
          :data-group="key"
          :value="key"
          v-model="selection"
          @change="emitSelection"
        />
      </div>
      <div class="text-item" v-else>
        <svg
          class="item-marker"
          width="16"
          height="16"
          viewBox="0 0 16 16"
          preserveXminYmin="true"
        >
          <circle cx="8" cy="8" r="8" :fill="data[key]" stroke-width="none" />
        </svg>
        <span class="item-label">
          {{ key }}
        </span>
      </div>
    </li>
  </ul>
</template>

<script>
// Create a legend for a visualisation for use outside the chart element. This
// component may be useful if you have several charts that display the
// same groups.
//
// @group VISUALISATIONS
export default {
  name: "ChartLegend",
  props: {
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

    // If `true`, click events will be enabled for all bars. When a bar is
    // clicked, the row-level data for that bar will be emitted.
    // To access the data, use the event `@barClicked=>(value) => ...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false,
    },
  },
  emits: ["legend-item-clicked"],
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
  },
  computed: {
    classNames() {
      const css = ["legend"];
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
.legend {
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
</style>
