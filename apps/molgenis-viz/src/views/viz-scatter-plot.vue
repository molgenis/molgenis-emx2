<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Scatter Plot"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'scatter-plot' }">Scatter Plot</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Scatter Plot</h2>
      <p>
        The <strong>scatter plot</strong> is ideal for displaying to continuous
        variables against each other. Support for grouping points is available
        by specifying the group to color mappings and with the legend (that's
        automatically generated).
      </p>
      <p>
        The example below demonstrates the scatter plot component and the
        grouping features.
      </p>
    </PageSection>
    <PageSection class="viz-section-dark" :verticalPadding="2">
      <MessageBox v-if="loading && !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <ScatterPlot
        v-else
        chartId="organisationsScatterPlot"
        title="Publication outputs by number of authors per institution and organisation type"
        :description="`For the ${data.length} institutions in the Netherlands, the total number of publishing authors and publications are displayed for Healthcare and Education institutions. (Note: values are randomly generated)`"
        :chartData="data"
        group="organisationType"
        xvar="authors"
        yvar="publications"
        xAxisLabel="Number of publishing authors"
        yAxisLabel="Number of publications"
        :chartMargins="{
          top: 15,
          right: 25,
          bottom: 60,
          left: 60,
        }"
        :enableClicks="true"
        :enableTooltip="true"
        @point-clicked="updateSelection"
      />
    </PageSection>
    <PageSection>
      <output class="output">
        {{ clicked }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { request } from "graphql-request";
import gql from "graphql-tag";
import { randomNormal } from "d3";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import ScatterPlot from "../components/viz/ScatterPlot.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/scatter-plot-header.jpg";

let loading = ref(true);
let error = ref(false);
let data = ref([]);
let clicked = ref({});

async function getOrganisations() {
  const query = gql`
    {
      Organisations(
        filter: {
          country: { equals: "Netherlands" }
          _or: [
            { organisationType: { equals: "Education" } }
            { organisationType: { equals: "Healthcare" } }
          ]
        }
      ) {
        name
        latitude
        longitude
        country
        organisationType
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const size = response.Organisations.length;
  const randPubsDist = Array.from({ length: size }, randomNormal(400, 80));
  const randAuthDist = Array.from({ length: size }, randomNormal(250, 50));
  data.value = response.Organisations.map((row, index) =>
    Object.assign(
      {},
      {
        ...row,
        publications: Math.floor(randPubsDist[index]),
        authors: Math.floor(randAuthDist[index]),
      }
    )
  );
}

function updateSelection(value) {
  clicked.value = value;
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
.viz-section-dark {
  background-color: $gray-700;
  color: $gray-000;
}
</style>
