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
    <PageSection class="viz-section">
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
      <PieChart
        v-else
        chartId="sexByPenguin"
        title="Summary of organisation type"
        :description="`In total, ${total} organisations across the Netherlands and Belgium were selected. The following chart shows the breakdown of oganisations by type.`"
        :chartData="data"
        :enableClicks="true"
        :chartHeight="200"
        @slice-clicked="updateSelection"
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
import { fetchData, asDataObject } from "@/utils/utils.js";
import { sum, format } from "d3"
const d3 = { sum, format }

import Page from "@/components/layouts/Page.vue";
import PageHeader from "@/components/layouts/PageHeader.vue";
import PageSection from "@/components/layouts/PageSection.vue";
import MessageBox from "@/components/display/MessageBox.vue";
import PieChart from "@/components/viz/PieChart.vue";
import Breadcrumbs from "@/app-components/breadcrumbs.vue";
import headerImage from "@/assets/sheri-silver-unsplash.jpg";

let loading = ref(true);
let hasError = ref(false);
let error = ref(null);
let selection = ref({});
let data = ref([]);
let total = ref(0);

const query = `{
  Statistics(filter: {component: {name: {equals: "organisations.by.type"}}}) {
    id
    label
    value
    valueOrder
    component {
      name
      definition
    }
  }
}`;

function updateSelection(value) {
  selection.value = value;
}

onMounted(() => {
  Promise.resolve(fetchData(query))
    .then((response) => {
      const format = d3.format(".2f");
      const totalOrgs = d3.sum(response.data.Statistics, row => row.value)
      const orgs = response.data.Statistics.map(row => {
        return {...row, rate: format((row.value / totalOrgs) * 100)}
      })
      
      total.value = totalOrgs
      data.value = asDataObject(orgs, "label", "rate")
      
      loading.value = false;
    })
    .catch((error) => {
      hasError.value = true;
      error.value = error;
    });
});
</script>
