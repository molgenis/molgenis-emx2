<template>
  <div class="d3-viz d3-column-chart">
    <h3 v-if="title" class="chart-title">{{ title }}</h3>
    <p v-if="description" class="chart-description">{{ description }}</p>
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
        <g class="chart-columns">
          <g
            class="column-group"
            v-for="row in chartData"
            :key="row[xvar]"
            :data-column="row[xvar]"
            :data-value="row[yvar]"
          >
            <rect
              :data-value="`${row[xvar]}-${row[yvar]}`"
              class="column"
              :x="xAxis(row[xvar])"
              :width="xAxis.bandwidth()"
              :fill="columnFill"
              @click="onClick(row)"
              @mouseover="(event) => onMouseOver(event)"
              @mouseleave="(event) => onMouseLeave(event)"
            />
            <text
              class="label"
              :x="xAxis(row[xvar])"
              :y="yAxis(row[yvar])"
              :dx="xAxis.bandwidth() / 2"
              dy="-3px"
            >
              {{ row[yvar] }}
            </text>
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
};

import { validateNumRange } from "../../utils/utils.js";

// Create a column chart (vertical bars) where the height of a bar
// is corresponds to a value of a categorical variable (along the x-axis). If
// you have many groups, consider using the `<BarChart>` component. You may also
// want to combine or exclude, groups with smaller values.
// @group VISUALISATIONS
export default {
  name: "ColumnChart",
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

    // the dataset the plot
    chartData: {
      type: Array,
      required: true,
    },

    // Name of the column that contains the groups to plot
    // along the x-axis
    xvar: {
      type: String,
      required: true,
    },
    // Name of the column that contains the values to plot
    // along the y-axis
    yvar: {
      type: String,
      required: true,
    },
    // Specify the max value of the y-axis. If left undefined,
    // max value will be automatically calculated using `d3.max`
    yMax: Number,

    // Specify the y-axis ticks
    yTickValues: Array,

    // A label that describes the x-axis
    xAxisLabel: String,

    // A label that describes the y-axis
    yAxisLabel: String,

    // If defined, x-axis labels will be split into multiple lines. Value must
    // be a separator that indicates where the string should be split. Please
    // be aware that you may need to adjust the chart margins and height
    // depending on how many lines you wish to break.
    xAxisLineBreaker: {
      type: String,
      default: null,
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
          left: 60,
        };
      },
    },

    // Set the fill of all columns (hex code)
    columnFill: {
      type: String,
      // `#6C85B5`
      default: "#6C85B5",
    },

    // Set the color that is displayed when a column is hovered (hex code)
    columnHoverFill: {
      type: String,
      // `#163D89`
      default: "#163D89",
    },

    // Adjust the amount of blank space inbetween columns between 0 and 1
    columnPaddingInner: {
      type: Number,
      // `0.2`
      default: 0.2,
      validator: (value) => validateNumRange(value),
    },

    // Adjust the amount of blank space before the first column and after
    // the last column. Value must be between 0 and 1
    columnPaddingOuter: {
      type: Number,
      // `0.2`
      default: 0.2,
      validator: (value) => validateNumRange(value),
    },

    // Along with `columnPaddingOuter`, specify how the columns are distributed
    // x-axis. A value of 0 will position the columns closer to the y-axis.
    columnAlign: {
      type: Number,
      // `0.5`
      default: 0.5,
      validator: (value) => validateNumRange(value),
    },
    // If `true`, click events will be enabled for all columns. When a column is
    // clicked, the row-level data for that column will be emitted.
    // To access the data, use the event `@column-clicked = ...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false,
    },

    // If `true`, columns will be drawn over 500ms from the x-axis.
    enableAnimation: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["column-clicked"],
  data() {
    return {
      chartWidth: 675,
    };
  },
  computed: {
    svgClassNames() {
      const css = ["chart"];
      if (this.title || this.description) {
        css.push("chart-has-context");
      }
      if (this.enableAnimation) {
        css.push("column-animation-enabled");
      }
      if (this.enableClicks) {
        css.push("column-clicks-enabled");
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
    yAxisMax() {
      return this.yMax
        ? this.yMax
        : d3.max(this.chartData, (row) => row[this.yvar]);
    },
    yAxis() {
      return d3
        .scaleLinear()
        .domain([0, this.yAxisMax])
        .range([this.heightMarginAdjusted, 0])
        .nice();
    },
    xAxis() {
      return d3
        .scaleBand()
        .range([0, this.widthMarginAdjusted])
        .domain(this.chartData.map((row) => row[this.xvar]))
        .paddingInner(this.columnPaddingInner)
        .paddingOuter(this.columnPaddingOuter)
        .round(true);
    },
    chartAxisX() {
      return d3.axisBottom(this.xAxis);
    },
    chartAxisY() {
      const axis = d3.axisLeft(this.yAxis);
      return this.yTickValues ? axis.tickValues(this.yTickValues) : axis;
    },
    chartColumns() {
      return this.chartArea.selectAll("rect.column");
    },
  },
  methods: {
    setChartDimensions() {
      const parent = this.$el.parentNode;
      this.chartWidth = parent.offsetWidth * 0.95;
    },
    breakXAxisLines() {
      const separator = this.xAxisLineBreaker;
      this.svg.selectAll(".chart-axis-x .tick text").call((labels) => {
        labels.each(function () {
          var node = d3.select(this);
          var stringArray = node.text().split(separator);
          node.text("");
          stringArray.forEach(function (str) {
            node.append("tspan").attr("x", 0).attr("dy", "1em").text(str);
          });
        });
      });
    },
    renderAxes() {
      this.chartArea.select(".chart-axis-x").call(this.chartAxisX);

      this.chartArea.select(".chart-axis-y").call(this.chartAxisY);

      if (typeof this.xAxisLineBreaker !== "undefined") {
        this.breakXAxisLines();
      }
    },
    onClick(row) {
      if (this.enableClicks) {
        const data = JSON.stringify(row);
        this.$emit("column-clicked", data);
      }
    },
    onMouseOver(event) {
      const column = event.target;
      const label = column.nextSibling;
      column.style.fill = this.columnHoverFill;
      label.style.opacity = 1;
    },
    onMouseLeave(event) {
      const column = event.target;
      const label = column.nextSibling;
      column.style.fill = this.columnFill;
      label.style.opacity = 0;
    },
    drawColumns() {
      const columns = this.chartColumns.data(this.chartData);
      if (this.enableAnimation) {
        columns
          .attr("height", 0)
          .attr("y", this.yAxis(0))
          .transition()
          .delay(200)
          .duration(500)
          .attr("y", (row) => this.yAxis(Math.max(0, row[this.yvar])))
          .attr("height", (row) =>
            Math.abs(this.yAxis(row[this.yvar]) - this.yAxis(0))
          );
      } else {
        columns
          .attr("y", (row) => this.yAxis(Math.max(0, row[this.yvar])))
          .attr("height", (row) =>
            Math.abs(this.yAxis(row[this.yvar]) - this.yAxis(0))
          );
      }
    },
    renderChart() {
      this.setChartDimensions();
      this.renderAxes();
      this.drawColumns();
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
.d3-column-chart {
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

    .chart-axes {
      .tick {
        font-size: 11pt;
      }
    }

    .chart-labels {
      font-size: 12pt;

      .chart-axis-title {
        text-anchor: middle;

        &.chart-axis-y {
          transform: rotate(-90deg);
          transform-origin: top left;
        }
      }
    }

    .column-group {
      .label {
        text-anchor: middle;
        font-size: 11pt;
        opacity: 0;
      }
    }

    &.column-clicks-enabled {
      rect.column {
        cursor: pointer;
      }
    }

    &.chart-has-context {
      margin-top: 12px;
    }
  }
}
</style>
