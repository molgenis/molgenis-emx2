<template>
  <div class="d3-viz d3-scatter-plot">
    <h3 v-if="title" class="chart-title">{{ title }}</h3>
    <p v-if="description" class="chart-description">{{ description }}</p>
    <ChartLegend
      v-if="enableChartLegend && group !== ''"
      :data="legendData"
      :stackLegend="stackLegend"
      :enableClicks="enableLegendClicks"
      @legend-item-clicked="setLegendClicked"
    />
    <div class="chart-container">
      <svg
        :id="chartId"
        :class="svgClassNames"
        width="100%"
        height="100%"
        :viewBox="viewBox"
        preserveAspectRatio="xMinYMin"
      >
        <g
          class="chart-area"
          :transform="`translate(${chartMargins.left}, ${chartMargins.top})`"
        >
          <g class="chart-axes">
            <g
              class="chart-axis-x"
              :transform="`translate(0,${heightMarginAdjusted})`"
            ></g>
            <g class="chart-axis-y"></g>
          </g>
          <g class="chart-points">
            <g class="point-group" v-for="row in chartData">
              <circle
                class="point"
                :data-group="group !== '' ? group : 'all'"
                :data-xvar="row[xvar]"
                :data-yvar="row[yvar]"
                :cx="xAxis(row[xvar])"
                :cy="yAxis(row[yvar])"
                :r="pointRadius"
                :fill="group !== '' ? palette(row[group]) : pointFill"
                :style="
                  group !== ''
                    ? `opacity: ${
                        legendSelection.indexOf(row[group]) > -1 ? 0 : 1
                      }`
                    : ''
                "
                @click="setClickedPoint(row)"
                @mouseover="onPointMouseOver(row)"
                @mouseleave="onPointMouseOut()"
              />
            </g>
          </g>
        </g>
        <g class="chart-labels">
          <text
            :x="chartWidth / 2"
            :y="chartHeight - chartMargins.bottom / 4.5"
            :dx="(chartMargins.left - chartMargins.right) / 2"
            class="chart-text chart-axis-title chart-axis-x"
            v-if="xlabel"
          >
            {{ xlabel }}
          </text>
          <text
            :x="-(chartHeight / 2.25)"
            :y="chartMargins.left / 5.1"
            class="chart-text chart-axis-title chart-axis-y"
            v-if="ylabel"
          >
            {{ ylabel }}
          </text>
        </g>
      </svg>
      <div
        v-if="enableTooltip && tooltipData"
        :id="`${chartId}-tooltip`"
        class="d3-viz-tooltip scatter-plot-tooltip"
        v-html="tooltipTemplate(tooltipData)"
        :style="tooltipPosition"
      ></div>
    </div>
  </div>
</template>

<script>
import {
  select,
  selectAll,
  scaleBand,
  axisBottom,
  max,
  min,
  scaleLinear,
  axisLeft,
  schemePuBuGn,
  scaleOrdinal,
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
  schemePuBuGn,
  scaleOrdinal,
};

import ChartLegend from "./ChartLegend.vue";

export default {
  name: "ScatterPlot",
  props: {
    // a unique ID for the chart
    chartId: {
      type: String,
      required: true,
    },
    // A title that describes the chart
    title: String,

    // Additional information to display below the title
    description: String,

    // the dataset to plot
    chartData: {
      type: Array,
      required: true,
    },

    // Name of the column that contains values to plot
    // along the x-axis
    xvar: {
      type: String,
      required: true,
    },

    // Name of the column that contains values to plot
    // along the y-axis
    yvar: {
      type: String,
      required: true,
    },

    // Name of the column that contains the groups that
    // will be used to differeniate the points
    group: {
      type: String,
      // `null`
      default: "",
    },

    // Specify the min value of the x-axis. If left undefined,
    // min value will be automatically calculated using `d3.min`
    xMin: {
      type: Number,
      // `0`
      default: 0,
    },

    // Specify the max value of the x-axis. If left undefined,
    // max value will be automatically calculated using `d3.max`
    xMax: {
      type: Number,
      // `0`
      default: 0,
    },

    // Specify the min value of the y-axis. If left undefined,
    // min value will be automatically calculated using `d3.min`
    yMin: {
      type: Number,
      // `0`
      default: 0,
    },

    // Specify the max value of the y-axis. If left undefined,
    // max value will be automatically calculated using `d3.max`
    yMax: {
      type: Number,
      // `0`
      default: 0,
    },

    // Specify the x-axis ticks
    xTickValues: Array,

    // Specify the y-axis ticks
    yTickValues: Array,

    // A label that describes the x-axis
    xAxisLabel: String,

    // A label that describes the y-axis
    yAxisLabel: String,

    // set the radius of the points
    pointRadius: {
      type: Number,
      // `5`
      default: 5,
    },

    // Set the fill for all points
    pointFill: {
      type: String,
      // `#6C85B5`
      default: "#6C85B5",
    },

    // An object containing a mapping between subcategories (`xvar`) and colors.
    // Subcategories must match the value in the data otherwise fills colors will
    // not be joined with the data. If you would like to change the labels, you
    // must recode the data before passing it into this component.
    pointFillPalette: {
      type: Object,
      default: {},
    },

    // set the height of the chart. Width is determined by the
    // dimensions of the parent container so that the chart is
    // responsive. If you would like to specify the width of the
    // chart, use CSS or adjusted the `chartHeight`.
    chartHeight: {
      type: Number,
      // `425`
      default: 425,
    },

    // adjust the chart margins
    chartMargins: {
      type: Object,
      // `{ top: 15, right: 15, bottom: 60, left: 60 }`
      default() {
        return {
          top: 15,
          right: 15,
          bottom: 60,
          left: 80,
        };
      },
    },

    // If `true`, click events will be enabled for all columns. When a column is
    // clicked, the row-level data for that column will be emitted.
    // To access the data, use the event `@column-clicked = ...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false,
    },

    // If `true`, tooltip events will be enabled for all points. When a point
    // is hovered, the x and y values are display, as well as the group if
    // defined
    enableTooltip: {
      type: Boolean,
      // `false`
      default: true,
    },

    // A function that controls the HTML content in the tooltip. The x and y
    // values are displayed by default, as well as the group (if defined).
    // However, you may specify the content in the body of the tooltip by defining
    // a new function. Row-level data can be accessed by supplying `row` in the
    // function. E.g., `(row) => { return ...}`.
    tooltipTemplate: {
      type: Function,
      // `<p>x: ${row[this.yvar]}</p><p>y: ${row[this.xvar]}</p>`
      default(row) {
        const html = [
          `<p>${this.xvar}: ${row[this.xvar]}</p>`,
          `<p>${this.yvar}: ${row[this.yvar]}</p>`,
        ];
        if (this.group) {
          html.push(`<p>${this.group}: ${row[this.group]}</p>`);
        }
        return html.join("");
      },
    },

    // If `true` (default), a legend will be rendered in the below the chart only
    // when a value is supplied to the `groups` property.
    // Use props `stackLegend`... to customise the legend. Additional styling
    // should be made in the css/scss file.
    enableChartLegend: {
      type: Boolean,
      // `true`
      default: true,
    },

    // If `true`, all legend items will be stacked (i.e., vertically arranged).
    stackLegend: {
      type: Boolean,
      default: false,
    },

    // If `true`, click events will be enabled for legend groups. This allows
    // you to toggle chart elements when a group is clicked.
    enableLegendClicks: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["point-clicked"],
  components: {
    ChartLegend,
  },
  data() {
    return {
      chartWidth: 675,
      legendSelection: [],
      tooltipData: null,
      tooltipPosition: null,
    };
  },
  computed: {
    svgClassNames() {
      const css = ["chart"];
      if (this.title || this.description) {
        css.push("chart-has-context");
      }
      if (this.enableClicks || this.enableTooltip) {
        css.push("point-events-enabled");
      }
      return css.join(" ");
    },
    svg() {
      return d3.select(`#${this.chartId}`);
    },
    chartArea() {
      return this.svg.select(".chart-area");
    },
    xlabel() {
      return this.xAxisLabel ? this.xAxisLabel : false;
    },
    ylabel() {
      return this.yAxisLabel ? this.yAxisLabel : false;
    },
    widthMarginAdjusted() {
      return this.chartWidth - this.chartMargins.left - this.chartMargins.right;
    },
    heightMarginAdjusted() {
      return (
        this.chartHeight - this.chartMargins.top - this.chartMargins.bottom
      );
    },
    viewBox() {
      return `0 0 ${this.chartWidth} ${this.chartHeight}`;
    },
    xAxisMin() {
      return this.xMin
        ? this.xMin
        : d3.min(this.chartData, (row) => row[this.xvar]);
    },
    xAxisMax() {
      return this.xMax
        ? this.xMax
        : d3.max(this.chartData, (row) => row[this.xvar]);
    },
    yAxisMin() {
      return this.yMin
        ? this.yMin
        : d3.min(this.chartData, (row) => row[this.yvar]);
    },
    yAxisMax() {
      return this.yMax
        ? this.yMax
        : d3.max(this.chartData, (row) => row[this.yvar]);
    },
    xAxis() {
      return d3
        .scaleLinear()
        .domain([this.xAxisMin, this.xAxisMax])
        .range([0, this.widthMarginAdjusted])
        .nice();
    },
    yAxis() {
      return d3
        .scaleLinear()
        .domain([this.yAxisMin, this.yAxisMax])
        .range([this.heightMarginAdjusted, 0])
        .nice();
    },
    chartAxisX() {
      const axis = d3.axisBottom(this.xAxis);
      return this.xTickValues ? axis.tickValues(this.xTickValues) : axis;
    },
    chartAxisY() {
      const axis = d3.axisLeft(this.yAxis);
      return this.yTickValues ? axis.tickValues(this.yTickValues) : axis;
    },
    palette() {
      if (this.group !== "") {
        let domain, range;

        if (Object.keys(this.pointFillPalette).length) {
          domain = Object.keys(this.pointFillPalette);
          range = Object.keys(this.pointFillPalette).map(
            (key) => this.pointFillPalette[key]
          );
        } else {
          domain = [...new Set(this.chartData.map((row) => row[this.group]))];

          // domains less than 2 do not exist in the color scheme
          if (domain.length < 3) {
            range = d3.schemePuBuGn[3].slice(1);
          } else {
            range = d3.schemePuBuGn[domain.length];
          }
        }

        return d3.scaleOrdinal().domain(domain).range(range).unknown("#ccc");
      }
    },
    legendData() {
      if (this.group !== "") {
        const groups = [
          ...new Set(this.chartData.map((row) => row[this.group])),
        ];
        const data = groups.map((value) => [value, this.palette(value)]);
        return Object.fromEntries(data);
      }
    },
    points() {
      return this.chartArea.selectAll("circle.point");
    },
  },
  methods: {
    setChartDimensions() {
      const parent = this.$el.parentNode;
      this.chartWidth = parent.offsetWidth * 0.95;
    },
    renderAxes() {
      this.chartArea.select(".chart-axis-x").call(this.chartAxisX);

      this.chartArea.select(".chart-axis-y").call(this.chartAxisY);
    },
    setLegendClicked(value) {
      this.legendSelection = value;
    },
    setClickedPoint(value) {
      if (this.enableClicks) {
        this.$emit("point-clicked", value);
      }
    },
    onPointMouseOver(value) {
      this.tooltipData = value;

      const y = this.yAxis(value[this.yvar]);
      const x = this.xAxis(value[this.xvar]);

      let top, left;
      if (y >= this.chartHeight * 0.6) {
        top = y - 72;
      } else {
        top = y + 32;
      }
      if (x >= this.chartWidth * 0.6) {
        left = x - 150;
      } else {
        left = x + 75;
      }

      this.tooltipPosition = `top: ${top}px;left: ${left}px;`;
    },
    onPointMouseOut() {
      this.tooltipData = null;
    },
    renderChart() {
      this.setChartDimensions();
      this.renderAxes();
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
.d3-scatter-plot {
  position: relative;

  h3.chart-title {
    margin: 0;
    text-align: left;
    color: currentColor;
  }

  p.chart-description {
    margin: 0;
    text-align: left;
    color: currentColor;
  }

  .legend {
    margin: 0.4em 0;
  }

  .chart {
    display: block;
    margin: 0 auto;

    .chart-axes {
      .tick {
        font-size: 11pt;
      }
    }

    .chart-area {
      .point-group {
        .point {
          cursor: default;
        }

        .point-label {
          font-size: 11pt;
        }
      }
    }

    .chart-labels {
      font-size: 12pt;

      .chart-axis-title {
        text-anchor: middle;
        fill: currentColor;

        &.chart-axis-y {
          transform: rotate(-90deg);
          transform-origin: top left;
        }
      }
    }

    &.chart-has-context {
      margin-top: 12px;
    }

    &.point-events-enabled {
      circle {
        cursor: pointer;
      }
    }
  }

  .chart-container {
    position: relative;

    .chart {
      display: block;
      margin: 0;
    }

    .d3-viz-tooltip {
      position: absolute;
      top: 0;
      max-width: 300px;
      z-index: 10;
      color: $gray-700;
      background-color: $gray-000;
      box-shadow: 0 0 4px 2px $gray-transparent-400;
      border-radius: 3px;
      padding: 8px 12px;

      p {
        font-size: 11pt;
        padding: 0;
        margin: 0;
      }
    }
  }
}
</style>
