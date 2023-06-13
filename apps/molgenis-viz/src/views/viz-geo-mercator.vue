<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Map Component"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li><router-link :to="{name: 'geo-mercator'}">Map</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Map Example</h2>
      <p>The map component can be used to create a point location visualisation using a geomercator map from the D3 library where each point represents a unique location in the dataset. The map can be customised by adjusting the properties or using CSS.</p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <GeoMercator 
        v-else
        chartId="map"
        title="ROR Organisations"
        description="The following map shows organisations in Belgium, France, Germany, and the Netherlands by type (as defined in ROR: education, facility, and healthcare). Zoom in to explore the map and hover over a location to view more information. Click a location to view the row level data below. Click an item in the legend to hide or show the data on the map."
        rowId="name"
        :geojson="geojson"
        :chartData="data"
        latitude="lat"
        longitude="lng"
        groupingVariable="type" 
        :groupColorMappings="mapColorGroups"
        :legendData="mapColorGroups"
        :chartScale="2"
        :mapCenter="{latitude: 4.515, longitude: 49.205}"
        :zoomLimits="[0.2, 25]"
        :tooltipTemplate="(row) => {
          return `
            <p class='title'>${row.name}</p>
            <p class='location'>
              <span class='location-city'>${row.city}</span>
              <span class='location-type'>${row.type}</span>
            </p>
          `
        }"
        :enableLegendClicks="true"
        :enableMarkerClicks="true"
        @marker-clicked="updateSelection"
        />
    </PageSection>
    <PageSection>
    <output class="output">
      {{  location }}
    </output>
    </PageSection>
  </Page>
</template>

<script>
import Page from '@/components/layouts/Page.vue'
import PageHeader from '@/components/layouts/PageHeader.vue'
import PageSection from '@/components/layouts/PageSection.vue'
import MessageBox from '@/components/display/MessageBox.vue'
import GeoMercator from '@/components/viz/GeoMercator.vue'
import Breadcrumbs from '@/app-components/breadcrumbs.vue'

import { fetchData } from '@/utils/utils.js'
import geojson from '@/data/world.geo.json'

import headerImage from '@/assets/t-h-chia-unsplash.jpg'

export default {
  components: {
    Page,
    PageHeader,
    PageSection,
    MessageBox,
    GeoMercator,
    Breadcrumbs,
  },
  data () {
    return {
      headerImage: headerImage,
      loading: true,
      hasError: false,
      error: null,
      data: {},
      geojson: geojson,
      mapColorGroups: {
        'Education': '#F4D58D',
        'Facility': '#FC7573',
        'Healthcare': '#B6C2D9',
      },
      location: null
    }
  },
  methods: {
    updateSelection (data) {
      this.location = data
    }
  },
  mounted () {
    Promise.resolve(
      fetchData('/api/v2/rdcomponents_institutions?num=5000&q=lat!=""')
    ).then(response => {
      const data = response.items
      data.forEach(row => delete row['_href'])
      this.data = data
      this.loading = false
    }).catch(error => {
      this.loading = false
      this.hasError = true
      this.error = error
      throw new Error(error)
    })
  }
}
</script>
