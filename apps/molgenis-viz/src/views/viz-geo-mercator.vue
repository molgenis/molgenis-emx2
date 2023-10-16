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
        <li><router-link :to="{ name: 'geo-mercator' }">Map</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Map Example</h2>
      <p>
        The map component can be used to create a point location visualisation
        using a geomercator map from the D3 library where each point represents
        a unique location in the dataset. The map can be customised by adjusting
        the properties or using CSS.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading && !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
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
        latitude="latitude"
        longitude="longitude"
        groupingVariable="status"
        :groupColorMappings="mapColorGroups"
        :legendData="mapColorGroups"
        :chartScale="6"
        :mapCenter="{ latitude: 4.515, longitude: 51.55 }"
        :zoomLimits="[0.2, 25]"
        :tooltipTemplate="
          (row) => {
            return `
            <p class='title'>${row.name}</p>
            <p class='location'>
              ${row.city}, ${row.country}
              <span class='location-city'>${row.city}</span>
              <span class='location-country'>${row.country}</span>
            </p>
          `;
          }
        "
        :enableLegendClicks="true"
        :enableMarkerClicks="true"
        @marker-clicked="updateSelection"
      />
    </PageSection>
    <PageSection>
      <output class="output">
        {{ location }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { request } from "graphql-request";
import gql from "graphql-tag";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import GeoMercator from "../components/viz/GeoMercator.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/map-header.jpg";

import geojson from "../data/world.geo.json";

let loading = ref(true);
let error = ref(null);
let data = ref([]);
let location = ref(null);
const mapColorGroups = {
  Submitted: "#F4D58D",
  "No Data": "#B6C2D9",
};

function updateSelection(value) {
  location.value = value;
}

async function getOrganisations() {
  const query = gql`
    {
      Organisations(filter: { latitude: { between: [0, 100] } }) {
        name
        code
        ontologyTermURI
        country
        city
        latitude
        longitude
        providerInformation {
          hasSubmittedData
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  data.value = response.Organisations.map((row) => {
    return {
      ...row,
      status: row.providerInformation[0].hasSubmittedData
        ? "Submitted"
        : "No Data",
    };
  });
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
