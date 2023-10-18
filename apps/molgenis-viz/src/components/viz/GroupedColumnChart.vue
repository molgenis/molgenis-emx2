<template>
  <div class="d3-viz d3-grouped-column-chart">
    <h3 v-if="title" class="chart-title">{{ title }}</h3>
    <p v-if="description" class="chart-description">{{ description }}</p>
    <ChartLegend
      v-if="enableChartLegend"
      :legendId="chartId"
      :data="legendData"
      :stackLegend="stackLegend"
      :enableClicks="enableLegendClicks"
      @legend-item-clicked="setLegendClicked"
    />
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
            v-for="group in groupedData"
            class="column-group"
            :transform="`translate(${xAxis(group[0])},0)`"
          >
            <g class="column-subgroup" v-for="subgroup in group[1]">
              <rect
                :data-group="group[0]"
                :data-x="subgroup[xvar]"
                :data-y="subgroup[yvar]"
                class="column"
                :x="xSubAxis(subgroup[xvar])"
                :width="xSubAxis.bandwidth()"
                :fill="palette(subgroup[xvar])"
                @click="onClick(subgroup)"
                @mouseover="(event) => onMouseOver(event)"
                @mouseleave="(event) => onMouseLeave(event)"
              />
              <text
                class="label"
                :x="xSubAxis(subgroup[xvar])"
                :y="yAxis(subgroup[yvar])"
                :dx="xSubAxis.bandwidth() / 2"
                dy="-6px"
              >
                {{ subgroup[yvar] }}
              </text>
            </g>
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
  group,
  scaleOrdinal,
  schemePuBuGn,
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
  group,
  scaleOrdinal,
  schemePuBuGn,
};

import ChartLegend from "./ChartLegend.vue";
import { validateNumRange } from "../../utils/utils.js";

// Create a grouped column chart where the height of a column
// is corresponds to a value of a categorical variable (along the x-axis) and columns
// are arranged by a grouping variable. If you have many groups, consider
// using the `<GroupedBarChart>` component. You may also
// want to combine or exclude groups with smaller values.
// @group VISUALISATIONS

export default {
  name: "GroupedColumnChart",
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

    // Name of the column that contains the groups
    group: {
      type: String,
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

    // An object containing a mapping between subcategories (`xvar`) and colors.
    // Subcategories must match the value in the data otherwise fills colors will
    // not be joined with the data. If you would like to change the labels, you
    // must recode the data before passing it into this component.
    columnFillPalette: {
      type: Object,
      default: {},
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

    // If `true` (default), a legend will be rendered in the below the chart.
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
  emits: ["column-clicked"],
  components: {
    ChartLegend,
  },
  data() {
    return {
      chartWidth: 675,
      legendSelection: [],
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

    // render axis label if defined
    xlabel() {
      return this.xAxisLabel ? this.xAxisLabel : false;
    },
    ylabel() {
      return this.yAxisLabel ? this.yAxisLabel : false;
    },

    // recalculate chart deminsions using margins
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

    // define y-axis
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

    // define data for both x axes: x0 and x1
    groupedData() {
      return d3.group(this.chartData, (row) => row[this.group]);
    },
    categories() {
      return this.chartData.map((row) => row[this.group]);
    },
    subCategories() {
      return new Set(this.chartData.map((row) => row[this.xvar]));
    },

    xAxis() {
      return d3
        .scaleBand()
        .domain(this.categories)
        .rangeRound([0, this.widthMarginAdjusted])
        .paddingInner(this.columnPaddingInner)
        .paddingOuter(this.columnPaddingOuter);
    },

    xSubAxis() {
      return d3
        .scaleBand()
        .domain(this.subCategories)
        .range([0, this.xAxis.bandwidth()])
        .paddingInner(this.columnPaddingInner);
    },

    // create color palette for grouping variable and legend
    palette() {
      const domain = Object.keys(this.columnFillPalette).length
        ? Object.keys(this.columnFillPalette)
        : this.subCategories;

      const range = Object.keys(this.columnFillPalette).length
        ? Object.keys(this.columnFillPalette).map(
            (key) => this.columnFillPalette[key]
          )
        : d3.schemePuBuGn[this.subCategories.size];

      const scale = d3
        .scaleOrdinal()
        .domain(domain)
        .range(range)
        .unknown("#ccc");
      return scale;
    },

    legendData() {
      const data = new Array(...this.subCategories).map((value) => [
        value,
        this.palette(value),
      ]);
      return Object.fromEntries(data);
    },

    // render axes to svg area
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

    // define events: click and mouse events
    onClick(row) {
      if (this.enableClicks) {
        const data = JSON.stringify(row);
        this.$emit("column-clicked", data);
      }
    },

    setLegendClicked(value) {
      this.legendSelection = value;
      this.drawColumns();
    },

    onMouseOver(event) {
      const column = event.target;
      const text = column.nextSibling;
      column.style.fill = this.columnHoverFill;
      text.style.opacity = 1;
    },

    onMouseLeave(event) {
      const column = event.target;
      const text = column.nextSibling;
      const value = event.target.getAttribute("data-x");
      column.style.fill = this.palette(value);
      text.style.opacity = 0;
    },

    // function that handles the height calcuation if animations are enabled
    calculateColumnHeight(xvalue, yvalue) {
      const height = Math.abs(this.yAxis(yvalue) - this.yAxis(0));
      if (this.enableLegendClicks && this.legendSelection.length) {
        if (this.legendSelection.indexOf(xvalue) > -1) {
          return 0;
        }
        return height;
      }
      return height;
    },

    drawColumns() {
      let columns = this.chartColumns.data(this.chartData);
      if (this.enableAnimation) {
        columns = columns
          .attr("height", 0)
          .attr("y", this.yAxis(0))
          .transition()
          .delay(200)
          .duration(500);
      }

      columns
        .attr("y", (row) => this.yAxis(Math.max(0, row[this.yvar])))
        .attr("height", (row) =>
          this.calculateColumnHeight(row[this.xvar], row[this.yvar])
        );
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
.d3-grouped-column-chart {
  h3.chart-title {
    margin: 0;
    text-align: left;
  }

  p.chart-description {
    text-align: left;
    margin: 0;
  }

  .legend {
    margin: 0.4em 0;
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
