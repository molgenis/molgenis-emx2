<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Pie Chart"
      :imageSrc="headerImage"
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
      <h2>PieChart Component</h2>
      <p>
        The <strong>PieChart</strong> component is used to descriptives for
        categorical data. Input data must be an object with one or more
        key-value pairs. It is recommended to supply no more than 7 categories
        and to combine smaller groups into an "other" category. If you need to
        display more groups, it is strongly recommended to use the
        <strong>BarChart</strong> or <strong>ColumnChart</strong> components.
        Alternatively, the <strong>DataTable</strong> component is much better.
      </p>
      <p>
        It is also possible to enable click events to enhance interactivity with
        other visualisation components. See the example below.
      </p>
    </PageSection>
    <PageSection class="bkg-light" :verticalPadding="2">
      <MessageBox v-if="loading && !error">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && error" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <PieChart
        v-else
        chartId="organisationsByType"
        title="Summary of organisation type"
        :description="`${total} organisations are included in the dataset. The following chart shows the breakdown of oganisations by type.`"
        :chartData="data"
        :enableClicks="true"
        :chartHeight="250"
        :asDonutChart="true"
        @slice-clicked="updateSelection"
      />
      <h3>Selected Item</h3>
      <p>Click a slice in the chart of above to display the row-level data</p>
      <output class="output">
        {{ selection }}
      </output>
    </PageSection>
    <PageSection>
      <h2>Further Information</h2>
      <p>
        Since the development of the <strong>PieChart</strong> component, a
        number of design and implementation issues arose. As a result, the
        <router-link :to="{ name: 'pie-chart-2' }">PieChart2</router-link>
        component was created. Both components will work but version 1 will no
        longer be developed.
      </p>
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
import PieChart from "../components/viz/PieChart.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/pie-chart-header.jpg";

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
