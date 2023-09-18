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
        :description="`In total, ${total} organisations across the Netherlands and Belgium were selected. The following chart shows the breakdown of oganisations by type.`"
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
import { fetchData, asDataObject } from "../utils/utils.js";
import { sum, rollups } from "d3";
const d3 = { sum, rollups };

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import PieChart2 from "../components/viz/PieChart2.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import headerImage from "../assets/pie-chart-header.jpg";

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let selection = ref({});
let data = ref({});
let total = ref(0);

const query = `{
  Organisations {
    name
    organisationType
  }
}`;

function updateSelection(value) {
  selection.value = value;
}

onMounted(() => {
  Promise.resolve(fetchData("/api/graphql", query))
    .then((response) => {
      const rawdata = response.data.Organisations;
      const size = rawdata.length;
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
              percent: Math.round((group[1] / size) * 100),
            })
        )
        .sort((a,b) => {
          return a.percent < b.percent;
        })

      total.value = size;
      data.value = asDataObject(orgs, "type", "percent");

      loading.value = false;
    })
    .catch((error) => {
      hasError.value = true;
      error.value = error;
    });
});
</script>
