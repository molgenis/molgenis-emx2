<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Column Chart Example"
      imageSrc="column-chart-header.jpg"
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
      <MessageBox v-if="loading & !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
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
import { request } from "graphql-request";
import gql from "graphql-tag";
import { rollups } from "d3";
const d3 = { rollups };

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import MessageBox from "../components/display/MessageBox.vue";
import ColumnChart from "../components/viz/ColumnChart.vue";

let loading = ref(true);
let error = ref(null);
let selection = ref({});
let data = ref([]);

async function getOrganisations() {
  const query = gql`
    {
      Organisations {
        name
        organisationType
      }
    }
  `;
  const response = await request("../api/graphql", query);
  data.value = d3
    .rollups(
      response.Organisations,
      (row) => row.length,
      (row) => row.organisationType
    )
    .map((group) => new Object({ type: group[0], count: group[1] }))
    .sort((a, b) => (a.type < b.type ? -1 : 1));
}

function updateClicked(value) {
  selection.value = value;
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
