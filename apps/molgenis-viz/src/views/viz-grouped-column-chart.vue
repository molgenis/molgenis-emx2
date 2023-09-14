<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Grouped Column Chart Example"
      :imageSrc="headerImage"
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
        The <strong>ColumnChart</strong> component is used to display values for
        categorical data. Groups are plotted along the x-axis and values along
        the y-axis. If you would like to display values horizontally, use the
        <router-link :to="{ name: 'bar-chart' }">Bar Chart</router-link>
        component.
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
        title="Institutions by country and organisation type"
        description=""
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
    <PageSection>
      <GroupedColumnChart
        chartId="test-grouped-column-chart"
        :chartData="[
          {group: 'Group A', subgroup: 'Healthcare', value: 60},
          {group: 'Group A', subgroup: 'Education', value: 55},
          {group: 'Group A', subgroup: 'Facility', value: 25},
          {group: 'Group B', subgroup: 'Healthcare', value: 3},
          {group: 'Group B', subgroup: 'Education', value: 93},
          {group: 'Group B', subgroup: 'Facility', value: 66},
          {group: 'Group C', subgroup: 'Healthcare', value: 43},
          {group: 'Group C', subgroup: 'Education', value: 57},
          {group: 'Group C', subgroup: 'Facility', value: 49}
        ]"
        group="group"
        xvar="subgroup"
        yvar="value"
      />
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import MessageBox from "../components/display/MessageBox.vue";
import GroupedColumnChart from "../components/viz/GroupedColumnChart.vue";

import headerImage from "../assets/grouped-column-chart-header.jpg";

import { fetchData } from "../utils/utils.js";
import { rollups, rollup } from "d3";
const d3 = { rollups, rollup };

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let data = ref([]);
let clicked = ref({});

function updateClicked(data) {
  clicked.value = data;
}

const query = `{
  Organisations {
    name
    code
    city
    country
    latitude
    longitude
    organisationType
    providerInformation {
      hasSubmittedData
    }
  }
}`;

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

onMounted(() => {
  Promise.resolve(fetchData("/api/graphql", query))
    .then((response) => {
      const data = response.data.Organisations;
      const summarized = d3.rollup(
        data,
        (row) => row.length,
        (row) => row.country,
        (row) => row.organisationType
      );
      return unroll(summarized, ["country", "organisationType"], "count");
    })
    .then((result) => {
      data.value = result;
      loading.value = false;
    })
    .catch((error) => {
      const err = error.message;
      loading.value = false;
      hasError.value = true;
      error.value = err;
      throw new Error(error);
    });
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
