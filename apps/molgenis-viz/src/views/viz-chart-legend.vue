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
        assigned unique colors to the examples dataset using d3-scale-chromatic.
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
          @legend-item-clicked="updateSelection"
        />
      </div>
      <p>
        Click events are also enabled. This allows users to interact with the
        visualisation components by filtering the data. Click an item to view
        the "filtered" state.
      </p>
      <output class="output">
        {{ selection }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { schemeGnBu } from "d3-scale-chromatic";
import { fetchData } from "../utils/utils";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import ChartLegend from "../components/viz/ChartLegend.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/studio-media-unsplash.jpg";

let loading = ref(false);
let data = ref([]);
let error = ref(null);
let selection = ref([]);

function updateSelection(value) {
  selection.value = value;
}

const query = `{
  Statistics (filter: { component: {name: {equals:"organisations.by.type"}}}) {
    label
    component {
      name
    }
  }
}`;

onMounted(() => {
  Promise.resolve(fetchData(query))
    .then((response) => {
      const stats = response.data.Statistics;
      const groups = [...new Set(stats.map((row) => row.label))];
      const scheme = schemeGnBu[groups.length];
      const colors = {};
      groups.forEach((key, index) => (colors[key] = scheme[index]));

      data.value = colors;
      loading.value = false;
    })
    .catch((error) => {
      loading.value = false;
      error.value = error;
    });
});
</script>

<style lang="scss">
#legendContainer {
  padding: 1em;
  background-color: $gray-700;
  color: $gray-050;
}
</style>
