<template>
  <div class="d3-viz d3-gauge">
    <h3 v-if="title" class="chart-title">{{ title }}</h3>
    <p v-if="description" class="chart-description">{{ description }}</p>
    <svg
      :id="chartId"
      :class="svgClassNames"
      width="100%"
      height="100%"
      :viewBox="viewbox"
      preserveAspectRatio="xMinYMin"
    >
      <g
        class="chart-area"
        :transform="`translate(${chartWidth / 2}, ${chartHeight / 2})`"
      >
        <g class="gauge-base-layer"></g>
        <g class="gauge-value-layer"></g>
        <g class="gauge-text-layer"></g>
      </g>
    </svg>
  </div>
</template>

<script>
import { select, arc, pie } from "d3";
const d3 = { select, arc, pie };

import { validateNumRange } from "../../utils/utils.js";

export default {
  name: "GaugeChart",
  props: {
    // A unique ID for the chart
    chartId: {
      type: String,
      required: true,
    },

    // A title that describes the chart
    title: String,

    // Additional information to display below the title
    description: String,

    // A decimal between 0 and 1
    value: {
      type: Number,
      required: true,
      validator: (value) => validateNumRange(value),
    },

    // Set the color of the foreground layer (i.e., value)
    valueFill: {
      type: String,
      // `#1c9099`
      default: "#01665e",
    },

    // Set the color of the background layer
    baseFill: {
      type: String,
      // default: '#c7eae5'
      default: "#c7eae5",
    },

    // set the height of the chart. Width is determined by the
    // dimensions of the parent container so that the chart is
    // responsive. If you would like to specify the width of the
    // chart, use CSS or adjusted the `chartHeight`.
    chartHeight: {
      type: Number,
      // `300`
      default: 300,
    },

    // set all chart margins
    chartMargins: {
      type: Number,
      // `10`
      default: 10,
    },

    // set chart scale
    chartScale: {
      type: Number,
      // `0.75`
      default: 0.75,
      validator: (value) => validateNumRange(value),
    },

    // If `true`, click events will be enabled for all bars. When a bar is
    // clicked, the row-level data for that bar will be emitted.
    // To access the data, use the event `@slice-clicked=...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: true,
    },
  },
  emits: ["arc-clicked"],
  data() {
    return {
      chartWidth: 300,
    };
  },

  computed: {
    svg() {
      return d3.select(`#${this.chartId}`);
    },
    svgClassNames() {
      const css = ["chart"];
      if (this.title || this.description) {
        css.push("chart-has-context");
      }

      if (this.enableClicks) {
        css.push("arc-clicks-enabled");
      }

      return css.join(" ");
    },
    chartArea() {
      return this.svg.select("g.chart-area");
    },
    viewbox() {
      return `0 0 ${this.chartWidth} ${this.chartHeight}`;
    },
    radius() {
      return (
        Math.min(this.chartWidth, this.chartHeight) / 2 - this.chartMargins
      );
    },
    pie() {
      return d3
        .pie()
        .sort(null)
        .value((value) => value[1]);
    },
    baseArc() {
      return d3
        .arc()
        .outerRadius(this.radius * 0.7)
        .innerRadius(this.radius * 0.4);
    },
    valueArc() {
      return d3
        .arc()
        .outerRadius(this.radius * 0.7)
        .innerRadius(this.radius * 0.4);
    },
    gaugeData() {
      return this.pie(
        Object.entries({ value: this.value, default: 1 - this.value })
      );
    },
  },
  methods: {
    setChartDimensions() {
      const parent = this.$el.parentNode;
      this.chartWidth = parent.offsetWidth * this.chartScale;
    },
    onClick(value) {
      const data = {};
      data[value.data[0]] = value.data[1];
      this.$emit("arc-clicked", data);
    },
    drawArcLayers() {
      const baseLayer = this.chartArea.select(".gauge-base-layer");
      baseLayer.selectAll("*").remove();
      baseLayer
        .selectAll("base")
        .data(this.gaugeData)
        .join("path")
        .attr("d", this.baseArc)
        .attr("class", (value) =>
          value.data[0] === "value" ? "arc-value" : "arc-default"
        )
        .attr("fill", this.baseFill);

      const valueLayer = this.chartArea.select(".gauge-value-layer");
      valueLayer.selectAll("*").remove();
      const values = valueLayer
        .selectAll("value")
        .data(this.gaugeData)
        .join("path")
        .attr("d", this.valueArc)
        .attr("class", (value) =>
          value.data[0] === "value" ? "arc-value" : "arc-default"
        )
        .attr("fill", (value) =>
          value.data[0] === "default" ? "none" : this.valueFill
        )
        .attr("stroke", (value) =>
          value.data[0] === "default" ? "none" : this.valueFill
        );

      const textLayer = this.chartArea.select(".gauge-text-layer");
      textLayer.selectAll("*").remove();
      textLayer
        .selectAll("text")
        .data(this.gaugeData)
        .join("text")
        .attr("dy", "0.35em")
        .attr("dx", "0.15em")
        .text((value) =>
          value.data[0] === "value" ? `${Math.round(value.value * 100)}%` : null
        );

      if (this.enableClicks) {
        values.on("click", (event, value) => this.onClick(value));
      }
    },
    renderChart() {
      this.setChartDimensions();
      this.drawArcLayers();
    },
  },
  mounted() {
    this.renderChart();
    window.addEventListener("resize", this.renderChart);
  },
  updated() {
    this.renderChart();
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.renderChart);
  },
};
</script>

<style lang="scss">
.d3-gauge {
  h3.chart-title {
    margin: 0;
    text-align: left;
  }

  p.chart-description {
    text-align: left;
    margin: 0;
  }

  .chart {
    display: block;
    margin: 0;

    .chart-area {
      .gauge-text-layer {
        font-size: 24pt;
        text-anchor: middle;
      }
    }

    &.arc-clicks-enabled {
      .arc-value {
        cursor: pointer;
      }
    }

    &.chart-has-context {
      margin-top: 12px;
    }
  }
}
</style>
