<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Bar Chart Example"
      imageSrc="bar-chart-header.jpg"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'bar-chart' }">Bar Chart</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection class="viz-section">
      <h2>Bar Chart</h2>
      <p>
        The <strong>BarChart</strong> component is used to display values for
        categorical data. Groups are plotted along the y-axis and values along
        the x-axis. If you would like to display values vertically, use the
        <router-link :to="{ name: 'column-chart' }">Column Chart</router-link>
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
      <BarChart
        v-else
        chartId="organisationsByType"
        title="Organistations by type"
        description="The following chart shows the number of organisations by type."
        :chartData="data"
        xvar="count"
        yvar="type"
        :chartMargins="{ left: 110, top: 10, right: 40, bottom: 60 }"
        :barPaddingInner="0.25"
        :barPaddingOuter="0.25"
        xAxisLabel="Number of Organisations"
        :enableClicks="true"
        @bar-clicked="updateClicked"
      />
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
import MessageBox from "../components/display/MessageBox.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import BarChart from "../components/viz/BarChart.vue";

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
    .sort((a, b) => (a.count < b.count ? 1 : -1));
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
