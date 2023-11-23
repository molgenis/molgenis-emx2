<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Chart Legends"
      imageSrc="legend-header.jpg"
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
import { request, gql } from "graphql-request";
import { schemeGnBu } from "d3-scale-chromatic";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import ChartLegend from "../components/viz/ChartLegend.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";

let loading = ref(false);
let data = ref({});
let error = ref(null);
let selection = ref([]);

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        organisationType
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const orgtypes = [
    ...new Set(response.Organisations.map((row) => row.organisationType)),
  ];
  const scheme = schemeGnBu[orgtypes.length];
  const colors = {};
  orgtypes.forEach((key, index) => (colors[key] = scheme[index]));
  data.value = colors;
}

function updateSelection(value) {
  selection.value = value;
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
#legendContainer {
  padding: 1em;
  background-color: $gray-700;
  color: $gray-050;
}
</style>
