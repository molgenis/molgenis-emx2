<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Chart Legends"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'chart-legend' }">Chart Legend</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection class="viz-section">
      <h2>rd-components: Chart legend example</h2>
      <p>
        The <strong>ChartLegend</strong> component is used to create legends for
        other visualisation components. The legend is rendered using an object
        containing one or more key-value pair. For example, in the box below, I
        assigned unique colors to the examples dataset using D3 scaleOrdinal and
        d3 color schemes.
      </p>
      <p>Here's the example input data.</p>
      <output class="output">
        {{ data }}
      </output>
      <p>This generates the following legend.</p>
      <MessageBox v-if="loading">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="error" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <div id="legendContainer" v-else>
        <ChartLegend
          :data="data"
          :stackLegend="true"
          :enableClicks="true"
          @legend-item-clicked="selection"
        />
      </div>
      <p>
        Click events are also enabled. This allows users to interact with the
        visualisation components by filtering the data. Click an item to view
        the "filtered" state.
      </p>
      <output class="output">
        {{ clicked }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref } from "vue";
import { request } from "graphql-request";

import { schemeGnBu } from "d3-scale-chromatic";
import { scaleOrdinal } from "d3";
const d3 = { schemeGnBu, scaleOrdinal };

import Page from "@/components/layouts/Page.vue";
import PageHeader from "@/components/layouts/PageHeader.vue";
import PageSection from "@/components/layouts/PageSection.vue";
import MessageBox from "@/components/display/MessageBox.vue";
import ChartLegend from "@/components/viz/ChartLegend.vue";
import Breadcrumbs from "@/app-components/breadcrumbs.vue";

import headerImage from "@/assets/studio-media-unsplash.jpg";

let loading = ref(false);
let data = ref([]);
let error = ref(null);
let selection = ref([])
let clicked = ref(null);

const query = `{
  Statistics(
    filter: { component: { name: { equals: "organisations.by.type" } } }
  ) {
    label
    value
    component {
      name
    }
  }
}
`

request("/dataviz/graphql", query)
.then(response => {
  console.log(response)
})
.catch((error) => {
    if (Array.isArray(error.response.errors)) {
      error.value = error.response.errors[0].message;
    } else {
      error.value = error;
    }
    loading.value = false;
  });
      // const data = response.items;
      // const groups = [...new Set(data.map((row) => row.island))];
      // const palette = d3.scaleOrdinal(d3.schemeGnBu[groups.length]);
      // const colors = {};
      // groups.forEach((key, index) => (colors[key] = palette(index)));
      // this.data = colors;
      // this.loading = false;

</script>

<style lang="scss">
#legendContainer {
  padding: 1em;
  background-color: $gray-700;
  color: $gray-050;
}
</style>
