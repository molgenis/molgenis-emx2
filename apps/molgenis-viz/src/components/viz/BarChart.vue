<template>
  <div class="d3-viz d3-bar-chart">
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
          <g class="chart-axis-x" :transform="`translate(0,${heightMarginAdjusted})`"></g>
          <g class="chart-axis-y"></g>
        </g>
        <g class="chart-bars">
          <g 
            class="bar-group"
            v-for="row in chartData"
            :key="row[yvar]"
            :data-column="row[yvar]"
            :data-value="row[xvar]"
          >
            <rect
              :data-value="`${row[yvar]}-${row[xvar]}`"
              class="bar"
              :y="yAxis(row[yvar])"
              :height="yAxis.bandwidth()"
              :fill="barFill"
              @click="onClick(row)"
              @mouseover="(event) => onMouseOver(event)"
              @mouseleave="(event) => onMouseLeave(event)"
            />
            <text
              class="label"
              :x="xAxis(row[xvar])"
              :y="yAxis(row[yvar])"
              dx="1.1em"
              :dy="(yAxis.bandwidth() / 1.65)"
            > 
              {{ row[xvar]}}
            </text>
          </g>
        </g>
      </g>
      <g class="chart-labels">
        <text 
          :x="(chartWidth / 2)"
          :y="chartHeight - (chartMargins.bottom / 4.5)"
          :dx="(chartMargins.left - chartMargins.right) / 2"
          class="chart-text chart-axis-title chart-axis-x"
          v-if="xlabel"
        >
          {{ xlabel }}
        </text>
        <text
          :x="-(chartHeight / 2.25)"
          :y="(chartMargins.left / 5.1)"
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
import { select, selectAll, scaleBand, axisBottom, max, min, scaleLinear, axisLeft } from 'd3'
const d3 = { select, selectAll, scaleBand, axisBottom, max, min, scaleLinear, axisLeft }

import { validateNumRange } from '$shared/js/d3.js'

// Create a bar chart (horizontal bars) that displays values along the
// x-axis by groups (along the y-axis). This component is ideal if you
// have many categorical variables or you would like to display ordinal
// data. However, if you many groups, consider combining or excluding
// groups with smaller values. If the labels excede the chart bounds,
// abbreviate labels and adjust the chart margins to allow for more space
// around the chart area. You may also want to consider describing the
// abbreviations in a figure caption or elsewhere on the page.
//
// @group VISUALISATIONS
export default {
  name: 'BarChart',
  props: {
    // a unique ID for the chart
    chartId: {
      type: String,
      required: true
    },
    // A title that describes the chart
    title: String,
    
    // Additional information to display below the title
    description: String,

    // Name of the column that contains the values to plot
    // along the x-axis
    xvar: {
      type: String,
      required: true
    },
    // Name of the column that contains the groups to plot
    // along the y-axis
    yvar: {
      type: String,
      required: true
    },
    // Specify the max value of the x-axis. If left undefined,
    // max value will be automatically calculated using `d3.max`
    xMax: Number,
    
    // Specify the x-axis ticks
    xTickValues: Array,
    
    // A label that describes the x-axis
    xAxisLabel: String,
    
    // A label that describes the y-axis
    yAxisLabel: String,
    
    // the dataset the plot
    chartData: {
      type: Array,
      required: true
    },
    
    // set the height of the chart. Width is determined by the
    // dimensions of the parent container so that the chart is
    // responsive. If you would like to specify the width of the
    // chart, use CSS or adjusted the `chartHeight`.
    chartHeight: {
      type: Number,
      // `425`
      default: 425
    },
    
    // adjust the chart margins
    chartMargins: {
      type: Object,
      // `{ top: 15, right: 15, bottom: 60, left: 65 }`
      default () {
        return {
          top: 15,
          right: 15,
          bottom: 60,
          left: 65
        }
      }
    },
    
    // Set the fill of all bars (hex code)
    barFill: {
      type: String,
      // `#6C85B5`
      default: '#6C85B5'
    },
    
    // Set the color that is displayed when a bar is hovered (hex code)
    barHoverFill: {
      type: String,
      // `#163D89`
      default: '#163D89'
    },
    
    // Adjust the amount of blank space inbetween bar between 0 and 1
    barPaddingInner: {
      type: Number,
      // `0.2`
      default: 0.2,
      validator: (value) => validateNumRange(value)
    },
    
    // Adjust the amount of blank space before the first bar and after
    // the last bar. Value must be between 0 and 1.
    barPaddingOuter: {
      type: Number,
      // `0.2`
      default: 0.2,
      validator: (value) => validateNumRange(value)
    },
    
    // Along with `barPaddingOuter`, specify how the bars are distributed
    // y-axis. A value of 0 will position the bars closer to the x-axis.
    barAlign: {
      type: Number,
      // `0.5`
      default: 0.5,
      validator: (value) => validateNumRange(value)
    },
    // If `true`, click events will be enabled for all bars. When a bar is
    // clicked, the row-level data for that bar will be emitted.
    // To access the data, use the event `@bar-clicked = ...`
    enableClicks: {
      type: Boolean,
      // `false`
      default: false
    },
    
    // If `true`, bars will be drawn over 500ms from the y-axis.
    enableAnimation: {
      type: Boolean,
      default: true
    },
  },
  emits: ['bar-clicked'],
  data () {
    return {
      chartWidth: 675,
    }
  },
  computed: {
    svgClassNames () {
      const css = ['chart']
      if (this.title || this.description) {
        css.push('chart-has-context')
      }
      if (this.enableAnimation) {
        css.push('bar-animation-enabled')
      }
      if (this.enableClicks) {
        css.push('bar-clicks-enabled')
      }
      return css.join(' ')
    },
    svg () {
      return d3.select(`#${this.chartId}`)
    },
    chartArea () {
      return this.svg.select('.chart-area')
    },
    xlabel () {
      return this.xAxisLabel ? this.xAxisLabel : false
    },
    ylabel () {
      return this.yAxisLabel ? this.yAxisLabel : false
    },
    widthMarginAdjusted () {
      return this.chartWidth - this.chartMargins.left - this.chartMargins.right
    },
    heightMarginAdjusted () {
      return this.chartHeight - this.chartMargins.top - this.chartMargins.bottom
    },
    viewBox () {
      return `0 0 ${this.chartWidth} ${this.chartHeight}`
    },
    xAxisMax () {
      return this.xMax ? this.xMax : d3.max(this.chartData, row => row[this.xvar])
    },
    xAxis () {
      return d3.scaleLinear()
        .domain([0, this.xAxisMax])
        .range([0, this.widthMarginAdjusted])
        .nice()
    },
    yAxis () {
      return d3.scaleBand()
        .range([0, this.heightMarginAdjusted])
        .domain(this.chartData.map(row => row[this.yvar]))
        .paddingInner(this.barPaddingInner)
        .paddingOuter(this.barPaddingOuter)
        .round(true)
    },
    chartAxisX () {
      const axis = d3.axisBottom(this.xAxis)
      return this.xTickValues ? axis.tickValues(this.xTickValues) : axis
    },
    chartAxisY () {
      return d3.axisLeft(this.yAxis)
    },
    chartBars () {
      return this.chartArea.selectAll('rect.bar')
    }
  },
  methods: {
    setChartDimensions () {
      const parent = this.$el.parentNode
      this.chartWidth = parent.offsetWidth * 0.95
    },
    renderAxes () {
      this.chartArea.select('.chart-axis-x')
        .call(this.chartAxisX)

      this.chartArea.select('.chart-axis-y')
        .call(this.chartAxisY)
    },
    onClick (row) {
      if (this.enableClicks) {
        const data = JSON.stringify(row)
        this.$emit('bar-clicked', data)
      }
    },
    onMouseOver (event) {
      const column = event.target
      const label = column.nextSibling
      column.style.fill = this.columnHoverFill
      label.style.opacity = 1
    },
    onMouseLeave (event) {
      const column = event.target
      const label = column.nextSibling
      column.style.fill = this.columnFill
      label.style.opacity = 0
    },
    drawBars () {
      const bars = this.chartBars.data(this.chartData)
      if (this.enableAnimation) {
        bars.attr('width', 0)
          .attr('x', this.xAxis(0))
          .transition()
          .delay(200)
          .duration(500)
          .attr('x', row => this.xAxis(Math.min(0, row[this.xvar])))
          .attr('width', row => Math.abs(this.xAxis(row[this.xvar]) - this.xAxis(0)))
      } else {
        bars
          .attr('x', row => this.xAxis(Math.min(0, row[this.xvar])))
          .attr('width', row => Math.abs(this.xAxis(row[this.xvar]) - this.xAxis(0)))
      }
    },
    renderChart () {
      this.setChartDimensions()
      this.renderAxes()
      this.drawBars()
    }
  },
  mounted () {
    this.renderChart()
    window.addEventListener('resize', this.renderChart)
  },
  updated () {
    this.renderChart()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.renderChart)
  }
}
</script>

<style lang="scss">
.d3-bar-chart {
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
    
    .bar-group {
      .label {
        text-anchor: middle;
        font-size: 11pt;
        opacity: 0;
      }
    }
    
    &.bar-clicks-enabled {
      rect.bar {
        cursor: pointer;
      }
    }
    
    &.chart-has-context {
      margin-top: 12px;
    }
  }
}
</style>
