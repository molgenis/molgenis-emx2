<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Grouped Column Chart Example"
      imageSrc="grouped-column-chart-header.jpg"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'grouped-column-chart' }">
            Grouped Column Chart
          </router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection :verticalPadding="2">
      <h2>Column Chart example</h2>
      <p>
        The <strong>GroupedColumnChart</strong> component is used to display
        values for categorical data by group. Groups are plotted along the
        x-axis and values along the y-axis. Legends are shown by default and can
        be used to hide/show groups.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <GroupedColumnChart
        v-else
        chartId="institutionsByCountryAndType"
        title="Number of organisations by country and organisation type"
        description="Of the four countries included in the dataset, France has the most 'facilities'."
        :chartData="data"
        group="country"
        :columnFillPalette="{
          Healthcare: '#b2e2e2',
          Education: '#66c2a4',
          Facility: '#238b45',
        }"
        xvar="organisationType"
        yvar="count"
        :chartMargins="{ left: 60, top: 10, right: 40, bottom: 80 }"
        :enableClicks="true"
        @column-clicked="updateClicked"
      />
      <h3>Selected Item</h3>
      <p>Click a column in the chart of above to display the row-level data</p>
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

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import MessageBox from "../components/display/MessageBox.vue";
import GroupedColumnChart from "../components/viz/GroupedColumnChart.vue";

import { rollup } from "d3";
const d3 = { rollup };

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let data = ref([]);
let clicked = ref({});

function updateClicked(data) {
  clicked.value = data;
}

function unroll(rollup, keys, label = "value", p = {}) {
  return Array.from(rollup, ([key, value]) =>
    value instanceof Map
      ? unroll(
          value,
          keys.slice(1),
          label,
          Object.assign({}, { ...p, [keys[0]]: key })
        )
      : Object.assign({}, { ...p, [keys[0]]: key, [label]: value })
  ).flat();
}

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        country
        organisationType
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const summarized = d3.rollup(
    response.Organisations,
    (row) => row.length,
    (row) => row.country,
    (row) => row.organisationType
  );
  data.value = unroll(summarized, ["country", "organisationType"], "count");
}

onMounted(() => {
  getOrganisations()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
.output {
  display: block;
  width: 100%;
  box-sizing: content-box;
  padding: 1em;
  background-color: $gray-050;
}
</style>
