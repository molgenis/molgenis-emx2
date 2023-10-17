<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Pie Chart"
      imageSrc="pie-chart-header.jpg"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'pie-chart' }">Pie Chart</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>PieChart2 Component</h2>
      <p>
        After implementing the
        <router-link :to="{ name: 'pie-chart' }">PieChart</router-link>
        component, in several projects, it was apparent that the first iteration
        did not meet the project-specific demands. For example, some projects
        have very long names or they require more than the recommended number of
        categories. To address these issues, the <strong>PieChart2</strong> was
        created. The second iteration introduces a new legend feature and a
        number of other options to interact with the chart.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <PieChart2
        v-else
        chartId="organisationsByType2"
        title="Summary of organisation type"
        :description="`${total} organisations are included in the dataset. The following chart shows the breakdown of oganisations by type.`"
        :chartData="data"
        :enableClicks="true"
        :enableLegendHovering="true"
        :chartHeight="250"
        :asDonutChart="true"
        @slice-clicked="updateSelection"
        legend-position="bottom"
      />
      <h3>Selected Item</h3>
      <p>Click a slice in the chart of above to display the row-level data</p>
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
import { asDataObject } from "../utils/utils.js";
import { sum, rollups } from "d3";
const d3 = { sum, rollups };

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import PieChart2 from "../components/viz/PieChart2.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";

let loading = ref(true);
let error = ref(null);
let selection = ref({});
let data = ref({});
let total = ref(0);

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
  const rawdata = response.Organisations;
  total.value = rawdata.length;
  const orgs = d3
    .rollups(
      rawdata,
      (row) => row.length,
      (row) => row.organisationType
    )
    .map(
      (group) =>
        new Object({
          type: group[0],
          percent: Math.round((group[1] / total.value) * 100),
        })
    )
    .sort((a, b) => (a.percent < b.percent ? 1 : -1));
  data.value = asDataObject(orgs, "type", "percent");
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
