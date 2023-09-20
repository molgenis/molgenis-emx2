<template>
  <div :class="parentClassNames">
    <div class="chart-context" v-if="title || description">
      <h3 v-if="title" class="chart-title">{{ title }}</h3>
      <p v-if="description" class="chart-description">{{ description }}</p>
    </div>
    <ChartLegend
      v-if="enableChartLegend"
      :legendId="chartId"
      :data="colors"
      :stackLegend="stackLegend"
      :enableClicks="false"
      :enableHovering="enableLegendHovering"
      @legend-item-clicked="setLegendClicked"
      @legend-item-mouseover="onMouseOver"
      @legend-item-mouseout="onMouseOut"
    />
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
        <g class="pie-slices"></g>
        <g class="pie-labels"></g>
      </g>
    </svg>
  </div>
</template>

<script>
import ChartLegend from "./ChartLegend.vue";
import { select, selectAll, scaleOrdinal, pie, arc, schemeBlues, sort } from "d3";
const d3 = { select, selectAll, scaleOrdinal, pie, arc, schemeBlues, sort };

import { validateNumRange } from "../../utils/utils.js";

// Create a pie chart to visually display subelements of your data in relation
// to the entire dataset. The data should contain no more than 7 elements and
// all group-values pairs that are less than 1% must be combined into an
// "other" category. Colors should be used to highlight interesting findings
// rather than emphasizing groups. However, if you require a group-based color
// scheme, make sure colors are accessible and use a *muted* color palette.
// Please note that group differences can be emphasized by enabling animations.
//
// @group VISUALISATIONS
export default {
  name: "PieChart2",
  props: {
    // A unique ID for the chart
    chartId: {
      type: String,
      required: true,
    },

    // A title that describes the chart
    title: {
      type: String,
      required: true
    },

    // Additional information to display below the title
    description: String,

    // An object containing 7 or fewer group-value pairs
    chartData: {
      type: Object,
      required: true,
      validator: (object) => {
        return Object.keys(object).length <= 7;
      },
    },
    
    // If true (default), values will be rendered with the labels
    valuesAreShown: {
      type: Boolean,
      // `true`
      default: true
    },
    
    // If true (default), labels will be generated with a percent sign.
    valuesArePercents: {
      type: Boolean,
      // `true`
      default: true
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

    // An object containing one-to-one mappings of groups to colors.
    // If colors are not defined, a default palette will be chosen for you.
    chartColors: {
      type: Object,
      // `{}`
      default: null,
    },

    // Set the border color for the slices
    strokeColor: {
      type: String,
      // `#3f454b`
      default: "#3f454b",
    },

    // If `true`, the pie chart will be rendered as a donut chart
    // (i.e., with the center cut out).
    asDonutChart: {
      type: Boolean,
      // `false`
      default: false,
    },

    // If true, the chart will be aligned to the center of the parent
    // component. Otherwise, the chart will be left aligned.
    centerAlignChart: {
      type: Boolean,
      // `false`
      default: false,
    },
    
    // If `true`, hover events will be enabled for all slices. A hover event
    // will highlight the hovered slice by increasing the font size of the
    // labels and increasing the size of the slice. The slice will return
    // the default state when it is no longer hovered.
    enableHoverEvents: {
      type: Boolean,
      // `false`
      default: false
    },

    // If `true`, click events will be enabled for all slices. When a slice is
    // clicked, the row-level data for that bar will be emitted.
    // To access the data, use the event `@slice-clicked=...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false,
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
      // `false`
      default: false,
    },
    
    
    // If the legend is enabled, you can specify the arrangement of the pie chart,
    // the legend, and the chart context (title and description). Options are
    // 'top', 'bottom', 'right', 'left'
    legendPosition: {
      type: String,
      // `top`
      default: 'top',
      validator: (value) => {
        return ['bottom', 'left', 'right', 'top'].includes(value);
      }
    },

    // If `true`, click events will be enabled for legend groups. This allows
    // you to toggle chart elements when a group is clicked.
    enableLegendClicks: {
      type: Boolean,
      default: false,
    },
    
    // If `true`, hover events will be enabled for legend items. This allows you
    // to extract the value of the hovered slice.
    enableLegendHovering: {
      type: Boolean,
      // `false`
      default: false
    }
  },
  emits: ["slice-clicked"],
  components: {
    ChartLegend
  },
  data() {
    return {
      chartWidth: 300,
      legendSelection: [],
    };
  },
  computed: {
    svg() {
      return d3.select(`#${this.chartId}`);
    },
    parentClassNames () {
      const css = ['d3-viz', 'd3-pie'];
      css.push(`legend-position-${this.legendPosition}`);
      return css.join(' ')
    },
    svgClassNames() {
      const css = ["chart"];
      if (this.title || this.description) {
        css.push("chart-has-context");
      }

      if (this.centerAlignChart) {
        css.push("chart-center-aligned");
      }

      if (this.enableHoverEvents) {
        css.push("slice-hover-enabled");
      }
      
      if (this.enableClicks) {
        css.push("slice-clicks-enabled");
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
      const pie = d3
        .pie()
        .sort(null)
        .value((value) => value[1]);
      return this.asDonutChart ? pie.padAngle(1 / this.radius) : pie;
    },
    pieChartData() {
      return this.pie(Object.entries(this.chartData));
    },
    arcGenerator() {
      const arc = d3.arc().outerRadius(this.radius * 0.7);
      return this.asDonutChart
        ? arc.innerRadius(this.radius * 0.4)
        : arc.innerRadius(0);
    },
    labelArcGenerator() {
      return this.asDonutChart
        ? d3
            .arc()
            .innerRadius(this.radius - 1)
            .outerRadius(this.radius - 1)
        : d3
            .arc()
            .innerRadius(this.radius * 0.8)
            .outerRadius(this.radius * 0.8);
    },
    colors() {
      if (!this.chartColors) {
        const autoColors = {};
        const length = Object.keys(this.chartData).length;
        const palette = d3.scaleOrdinal(d3.schemeBlues[length]);
        Object.keys(this.chartData).sort().forEach((key, index) => {
          autoColors[key] = palette(index);
        });
        return autoColors;
      }
      return this.chartColors;
    }
  },
  methods: {
    setChartDimensions() {
      const parent = this.$el.parentNode;
      this.chartWidth = parent.offsetWidth * this.chartScale;
    },
    setLabelPosition(value) {
      const position = this.labelArcGenerator.centroid(value);
      const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
      position[0] = this.radius * 0.99 * (angle < Math.PI ? 1 : -1);
      return position;
    },
    setTextAnchor(value) {
      const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
      return angle < Math.PI ? "start" : "end";
    },
    setOffsetX(value) {
      const angle = value.startAngle + (value.endAngle - value.startAngle) / 2;
      return angle < Math.PI ? "-1.1em" : "1.1em";
    },
    onMouseOver(value) {
      const slice = this.chartArea.select(`.slice[data-group="${value}"]`);
      slice.node().classList.add("slice-focused");
    },
    onMouseOut(value) {
      const slice = this.chartArea.select(`path.slice[data-group="${value}"]`);
      slice.node().classList.remove("slice-focused");
    },
    onClick(value) {
      const data = {};
      data[value.data[0]] = value.data[1];
      // When segement is clicked, the underlying data (category and value) is returned
      this.$emit("slice-clicked", data);
    },
    drawSlices() {
      const pieSlices = this.chartArea.select(".pie-slices");
      pieSlices.selectAll("*").remove();
      const slices = pieSlices
        .selectAll("slices")
        .data(this.pieChartData)
        .join("path")
        .attr("d", this.arcGenerator)
        .attr("class", "slice")
        .attr("stroke", this.strokeColor)
        .attr("data-group", (value) => value.data[0])
        .attr("fill", (value) => this.colors[value.data[0]]);

      if (this.enableHoverEvents) {
        slices.on("mouseover", (event, value) => this.onMouseOver(value.data[0]));
        slices.on("mouseout", (event, value) => this.onMouseOut(value.data[0]));
      }
        
      if (this.enableClicks) {
        slices.on("click", (event, value) => this.onClick(value));
      }
    },
    drawLabels() {
      const pieLabels = this.chartArea.select(".pie-labels");
      pieLabels.selectAll("*").remove();
      pieLabels
        .selectAll("pie-label-lines")
        .data(this.pieChartData)
        .join("polyline")
        .attr("class", "pie-label-line")
        .attr("stroke", this.strokeColor)
        .attr("data-group", (value) => value.data[0])
        .attr("points", (value) => {
          const centroid = this.arcGenerator.centroid(value);
          const outerCircleCentroid = this.labelArcGenerator.centroid(value);
          const labelPosition = this.labelArcGenerator.centroid(value);
          const angle =
            value.startAngle + (value.endAngle - value.startAngle) / 2;
          labelPosition[0] = this.radius * 0.95 * (angle < Math.PI ? 1 : -1);
          return [centroid, outerCircleCentroid, labelPosition];
        });

      const labels = pieLabels
        .selectAll("slice-labels")
        .data(this.pieChartData)
        .join("text")
        .attr("class", "pie-label-text")
        .attr("data-group", (value) => value.data[0])
        .attr("x", (value) => this.setLabelPosition(value)[0])
        .attr("y", (value) => this.setLabelPosition(value)[1])
        .style("text-anchor", this.setTextAnchor)
        .style("font-size", "11pt")
        .attr("dx", "0.1em")
        .attr("dy", "0.2em")
        .text((value) => {
          const text = value.data[1];
          if (this.valuesArePercents) {
            return `${text}%`;
          }
          return text;
        });
    },
    setLegendClicked(value) {
      this.legendSelection = value;
    },
    renderChart() {
      this.setChartDimensions();
      this.drawSlices();
      this.drawLabels();
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
.d3-pie {
  display: grid;
  gap: 0.1em;
  
  .legend {
    grid-area: legend;
  }
  
  &.legend-position-bottom {
    grid-template-areas: 
      "context"
      "chart"
      "legend";
      
      .chart-legend {
        justify-content: center;
      }
  }
  
  &.legend-position-left {
    grid-template-areas: 
      "context context"
      "legend chart";
  }
  
  &.legend-position-right {
    grid-template-areas: 
      "context context"
      "chart legend";
  }
  
  &.legend-position-top {
    grid-template-areas:
      "context"
      "legend"
      "chart";   
  }
  
  .chart-context {
    grid-area: context;
    
    h3.chart-title {
      margin: 0;
      text-align: left;
    }
  
    p.chart-description {
      text-align: left;
      margin: 0;
    }
  }

  .chart {
    display: block;
    grid-area: chart;

    .chart-area {
      .slice {
        stroke-width: 1px;
        opacity: 0.7;
        cursor: default;
        transition: all 250ms ease-in-out;

        &.slice-focused {
          transform: scale(1.2);
        }
      }

      .pie-label-line {
        fill: none;
        stroke-width: 1px;
      }
    }

    &.slice-clicks-enabled, .slice-hover-enabled {
      .slice {
        cursor: pointer;
      }
    }
  }
}
</style>
