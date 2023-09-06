<template>
  <div class="d3-viz d3-progress-meter">
    <h3 class="chart-title">
      <span class="label">{{ title }}</span>
      <span class="value">{{ valueLabel }}%</span>
    </h3>
    <svg 
      :id="chartId"
      :class="svgClassNames"
      width="100%"
      height="100%"
      :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
      preserveAspectRatio="xMinYMin"
      >
      <g class="chart-area" :transform="`translate(0, ${barHeight * 0.15})`">
        <rect
          :x="xAxis(0)"
          :y="yAxis(title)"
          :height="barHeight"
          :width="xAxis(totalValue) - 2"
          fill="none"
          :stroke="barFill"
          stroke-width="2"
        />
        <rect
          class="bar"
          :x="xAxis(0)"
          :y="yAxis(title)"
          :width="xAxis(value)"
          :height="barHeight"
          :fill="barFill"
          :stroke="barFill"
          stroke-width="2"
        />
      </g>
    </svg>
  </div>
</template>

<script>
import { select, scaleLinear, scaleBand, format } from "d3";
const d3 = { select, scaleLinear, scaleBand, format };

// Create a progress meter.
// @group VISUALISATIONS
export default {
  name: 'ProgressMeter',
  props: {
    // A unique ID for the chart
    chartId: {
      type: String,
      required: true,
    },

    // A title that describes the chart
    title: {
      type: String,
      required: true,
    },
    
    // value that indicates how much progress has been made
    value: Number,
    
    // the total possible value
    totalValue: Number,
    
    // Set the height of all bar
    barHeight: {
      type: Number,
      // `35`
      default: 15,
    },
    
    // Set the fill of all bars (hex code)
    barFill: {
      type: String,
      // `#6C85B5`
      default: "#6C85B5",
    },
    
    // If `true`, bars will be drawn over 500ms from the y-axis.
    enableAnimation: {
      type: Boolean,
      default: true,
    },
    
  },
  data () {
    return {
      chartWidth: 500,
    }
  },
  computed: {
    svg () {
      return d3.select(`#${this.chartId}`);
    },
    svgClassNames () {
      const css = ["chart"];
      return css.join(" ");
    },
    chartArea() {
      return this.svg.select("g.chart-area");
    },
    chartHeight () {
      return this.barHeight * 1.25;
    },
    xAxis () {
      return d3.scaleLinear()
        .domain([0, this.totalValue])
        .range([0, this.chartWidth]);
    },
    yAxis () {
      return d3.scaleBand()
        .range([0, this.chartHeight])
        .domain(this.title);
    },
    chartBar () {
      return this.chartArea.select("rect.bar");
    },
    valueLabel () {
      const floatFormat = d3.format('.1f');
      return floatFormat((this.value / this.totalValue) * 100);
    }
  },
  methods: {
    setChartDimensions() {
      const parent = this.$el.parentNode;
      this.chartWidth = parent.offsetWidth * 0.95;
    },
  },
  mounted () {
    this.setChartDimensions();
    window.addEventListener("resize", this.setChartDimensions);
  },
  updated () {
    this.setChartDimensions();
  },
  beforeUnmount () {
    this.setChartDimensions();
    window.removeEventListener("resize", this.setChartDimensions);
  },
}

</script>

<style lang="scss">
.d3-progress-meter {
  .chart-title {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    
    span {
      flex-grow: 1;
      font-size: 18pt;
      font-weight: 400;
    }
    
    .label {
      text-align: left;
    }
    
    .value {
      text-align: right;
    }
  }
  
  .chart {
    margin-top: 0.35em;
  }
}
</style>