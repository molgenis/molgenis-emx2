<template>
  <div class="d3-viz d3-geo-mercator">
    <div class="chart-context" v-if="title || description">
      <h3 v-if="title" class="chart-title">{{ title }}</h3>
      <p v-if="description" class="chart-description">{{ description }}</p>
    </div>
    <div class="chart-container">
      <svg
        :class="svgClassNames"
        :id="chartId"
        width="100%"
        :height="chartHeight"
        :viewbox="viewbox"
        preserveAspectRatio="xMinYMin"
      >
        <g class="background-layer">
          <rect
            width="100%"
            height="100%"
            x="0"
            y="0"
            :fill="mapColors.water"
          />
        </g>
        <g class="geojson-layer">
          <path
            v-for="feature in geojson.features" 
            :d="geoPath(feature)"
            :fill="mapColors.land"
            :stroke="mapColors.border"
          />
        </g>
        <g class="data-layer">
          <circle
            v-for="row in chartData"
            :id="row[rowId]"
            class="marker"
            :key="row[rowId]"
            :cx="projection([row[longitude], row[latitude]])[0]"
            :cy="projection([row[longitude], row[latitude]])[1]"
            :data-row-id="row[rowId]"
            :stroke="markerStroke"
            :stroke-width="strokeWidth"
            :r="radiusScaled"
            :fill="
              groupColorMappings && groupingVariable
              ? groupColorMappings[row[groupingVariable]]
              : markerColor
            "
            @click.native="enableMarkerClicks && onClick(row)"
            @mouseover.native="showTooltip && onMouseOver($event)"
            @mousemove.native="showTooltip && onMouseMove($event)"
            @mouseleave.native="showTooltip && onMouseLeave($event)"
            :style="enableLegendClicks && legendSelection.indexOf(row[groupingVariable])> -1 
              ? 'opacity: 0; cursor: default;'
              : 'opacity: 1; cursor: pointer;'
            "
          ></circle>
        </g>
      </svg>
      <div class="d3-viz-legend" v-if="legendData">
        <ChartLegend 
          :data="legendData"
          :enableClicks="enableLegendClicks"
          @legend-item-clicked="setLegendClicked"
        />
      </div>
    </div>
  </div>
  <div
    :id="`${chartId}-tooltip`"
    class="d3-viz-tooltip geo-mercator-tooltip"
    v-if="showTooltip && tooltipData"
    v-html="tooltipTemplate(tooltipData)"
    :style="tooltipPosition"
  />
</template>

<script>
import ChartLegend from '@/components/VizLegend.vue'
import { select, selectAll, geoMercator, geoNaturalEarth1, geoPath, json, zoom } from 'd3'
const d3 = { select, selectAll, geoMercator, geoNaturalEarth1, geoPath, json, zoom }

// Create a point location visualation using a geomercator map from the D3 library.
// Each point represents a unique location in the dataset.
//
// @group VISUALISATIONS
export default {
  name: 'GeoMercator',
  props: {
    // a unique identifier for the map
    chartId: {
      type: String,
      required: true
    },
    
    // A title that describes the chart
    title: String,
    
    // Additional information to display below the title
    description: String,

    // reference dataset for the base layer
    geojson: {
      type: Object,
      required: true
    },

    // the dataset containing all locations and coordinates
    chartData: {
      type: Array,
      required: true
    },

    // the column containing the identifiers of each location
    rowId: {
      type: String,
      required: true
    },

    // the name of the column containing the latitudes
    latitude: {
      type: String,
      required: true
    },
    
    // the name of the column containing the longitude
    longitude: {
      type: String,
      required: true
    },

    // the name of the column containing grouping information (i.e., how locations 
    // are related, categorised, etc.)
    groupingVariable: {
      type: String
    },
 
    // if grouping variable is supplied, color mappings can also be added here.
    // Input must be an object that maps each unique group to a colour.
    groupColorMappings: {
      type: Object
    },
    
    // Set the default fill of the map markers if no groupColorMappings are supplied
    markerColor: {
      type: String,
      default: '#f6f6f6'
    },
    
    // Set color of the marker borders
    markerStroke: {
      type: String,
      default: '#404040'
    },

    // set the height of the chart
    chartHeight: {
      type: Number,
      // `500`
      default: 500
    },

    // the point (lat, long) to the center map
    mapCenter: {
      type: Object,
      // `{latitude: 3.55, longitude: 47.55 }`
      default () { 
        return {
          latitude: 3.55,
          longitude: 47.55
        }
      }
    },

    // control the size of the chart
    chartSize: {
      type: Number,
      // `700`
      default: 400
    },

    // set the scale of the chart
    chartScale: {
      type: Number,
      // `1.1`
      default: 1.0
    },

    // set the radius of the points
    pointRadius: {
      type: Number,
      // `6`
      default: 6
    },
    
    // An object containing one or more key-value pairs where
    // each key is a group and the value is a color. See the
    // `<Legend/>` component for more information 
    legendData: {
      type: Object,
    },

    // If true (default), a tool will be displayed when hovering over a point
    showTooltip: {
      type: Boolean,
      // `true`
      default: true
    },

    // A function that controls the HTML content in the tooltip. The name
    // of the point is always displayed. However, you may specify the
    // content in the body of the tooltip. The default text is:
    // `<row-id>: <latitude>, <longitude>`. To modify the content, pass define
    // a new function. Row-level data can be accessed by supplying `row` in the
    // function. E.g., `(row) => { return ...}`.
    tooltipTemplate: {
      type: Function,
      // `<p>${row[this.rowId]}: ${row[this.latitude]}, ${row[this.longitude]}</p>`
      default (row) {
        return `<p>${row[this.rowId]}: ${row[this.latitude]}, ${row[this.longitude]}</p>`
      }
    },
    
    // If `true`, click events will be enabled for all markers. When a marker is
    // clicked, the row-level data for that bar will be emitted.
    // To access the data, use the event `@marker-clicked = someFunction()`
    enableMarkerClicks: {
      type: Boolean,
      // `false`
      default: false
    },
    
    // If `true`, click events will be enabled for the legend. When an item
    // is clicked, all markers will be hidden until clicked again.
    enableLegendClicks: {
      type: Boolean,
      default: false
    },

    // If true (default), the map can be zoomed in and out
    enableZoom: {
      type: Boolean,
      default: true
    },
    
    // An array containing two values that determine the min and max zoom limits
    // [minimum, maximum]
    zoomLimits: {
      type: Array,
    },

    // Set the colors of the land, borders, and water
    mapColors: {
      type: Object,
      // `{land: '#4E5327', border: '#757D3B', water: '#6C85B5'}`
      default () {
        return {
          land: '#4E5327',
          border: '#757D3B',
          water: '#6C85B5'
        }
      }
    }
  },
  components: { ChartLegend },
  emits: ['marker-clicked'],
  data () {
    return {
      strokeWidth: 1,
      mapMarkers: null,
      tooltipData: null,
      tooltipPosition: null,
      chartWidth: 500,
      radiusScaler: 1,
      legendSelection: [],
    }
  },
  computed: {
    svg () {
      return d3.select(`#${this.chartId}`)
    },
    svgClassNames () {
      const css = ['chart']
      if (this.title || this.description) {
        css.push('chart-has-context')
      }
      return css.join(' ')
    },
    geojsonLayer () {
      return this.svg.select('g.geojson-layer')
    },
    dataLayer () {
      return this.svg.select('g.data-layer')
    },
    projection () {
      return d3.geoNaturalEarth1()
        .center([this.mapCenter.latitude, this.mapCenter.longitude])
        .scale(this.chartWidth * this.chartScale)
        .translate([this.chartWidth / 2, this.chartHeight / 2])
    },
    geoPath () {
      return d3.geoPath().projection(this.projection)
    },
    viewbox () {
      return `0 0 ${this.chartWidth} ${this.chartHeight}`
    },
    radiusScaled () {
      return this.pointRadius / this.radiusScaler
    },
  },
  methods: {
    setChartDimensions () {
      const parent = this.$el.parentNode
      this.chartWidth = parent.offsetWidth
    },
    onClick(data) {
      this.$emit('marker-clicked', data)
    },
    onMouseOver (event) {
      const point = event.target
      point.setAttribute('r', (this.radiusScaled * 1.85) )
    },
    onMouseMove (event) {
      const id = event.target.getAttribute('data-row-id')
      const row = this.chartData.filter(row => row[this.rowId] == id)[0]
      this.tooltipData = row
      this.tooltipPosition = `
        top:${event.pageY + 16}px;
        left:${event.pageX + 24}px;
      `
    },
    onMouseLeave (event) {
      const elem = event.target
      this.tooltipData = null
      elem.setAttribute('r', this.radiusScaled)
    },
    setupZoom () {
      this.geojsonLayer.style('cursor', 'pointer')
      const zoom = d3.zoom().on('zoom', event => {
        this.radiusScaler = event.transform.k
        this.strokeWidth = 1 / event.transform.k
        this.geojsonLayer.attr('transform', event.transform)
        this.dataLayer.attr('transform', event.transform)
      })
      this.svg.call(this.zoomLimits ? zoom.scaleExtent(this.zoomLimits) : zoom)
    },
    setLegendClicked (value) {
      this.legendSelection = value
    },
    renderChart () {
      this.setChartDimensions()
      if (this.enableZoom) {
        this.setupZoom()
      }
    }
  },
  mounted () {
    this.renderChart()
    window.addEventListener('resize', this.renderChart)
  },
  updated () {
    this.renderChart()
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.renderChart)
  }
}
</script>

<style lang="scss">
.d3-geo-mercator {
  width: 100%;
  
  .chart-context {
    margin-bottom: 1em;
    
    h3.chart-title {
      margin: 0;    
      text-align: left;
    }
    
    p.chart-description {
      text-align: left;
      margin: 0;
    }
  }
  
  .chart-container {
    position: relative;

    .chart {
      display: block;
      margin: 0;
    }
    
    .d3-viz-legend {
      position: absolute;
      top: 0;
      left: 0;
      font-size: 11pt;
      color: $gray-050;
      border: 1px solid $gray-900;
      background-color: $gray-transparent-700;
      padding: 12px;
      box-shadow: 2px 4px 4px 2px hsla(0, 0%, 0%, 0.2)
    }
  }
}

.geo-mercator-tooltip {
  position: absolute;
  top: 0;
  max-width: 300px;
  z-index: 10;
  background-color: $gray-000;
  box-shadow: 0 0 4px 2px $gray-transparent-400;
  border-radius: 3px;
  padding: 8px 12px;

  p {
    font-size: 11pt;
    padding: 0;
    margin: 0;
    
    &.title {
      font-size: 11pt;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      line-height: 1.2;
      font-weight: bold;
    }
  }
}
</style>
