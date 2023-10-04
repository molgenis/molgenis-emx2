<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Column Chart Example"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'column-chart' }">Column Chart</router-link>
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
      <ColumnChart
        v-else
        chartId="organisationsByType"
        title="Organisations by type"
        description="The following chart shows the number of organisations by type."
        :chartData="data"
        xvar="type"
        yvar="count"
        :chartMargins="{ left: 110, top: 10, right: 40, bottom: 60 }"
        :barPaddingInner="0.25"
        :barPaddingOuter="0.25"
        yAxisLabel="Number of Organisations"
        :enableClicks="true"
        @column-clicked="updateClicked"
      />
      <h3>Selected Item</h3>
      <p>Click a bar in the chart of above to display the row-level data</p>
      <output class="output">
        {{ selection }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { rollups } from "d3";
const d3 = { rollups };

import { fetchData } from "../utils/utils.js";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import MessageBox from "../components/display/MessageBox.vue";
import ColumnChart from "../components/viz/ColumnChart.vue";
import headerImage from "../assets/column-chart-header.jpg";

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let selection = ref({});
let data = ref([]);

const query = `{
  Organisations {
    name
    organisationType
  }
}`;

function updateClicked(value) {
  selection.value = value;
}

onMounted(() => {
  Promise.resolve(fetchData("/api/graphql", query))
    .then((response) => {
      data.value = d3
        .rollups(
          response.data.Organisations,
          (row) => row.length,
          (row) => row.organisationType
        )
        .map((group) => new Object({ type: group[0], count: group[1] }));
      loading.value = false;
    })
    .catch((error) => {
      hasError.value = true;
      error.value = error;
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
