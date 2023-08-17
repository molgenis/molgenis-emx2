<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Bar Chart Example"
      :imageSrc="headerImage"
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
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
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
import { rollups } from "d3"

const d3 = { rollups };

import { fetchData, reverseSortData } from "../utils/utils.js";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import BarChart from "../components/viz/BarChart.vue";
import headerImage from "../assets/header-image.jpg";

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
  Promise.resolve(fetchData("/api/graphql",query))
    .then((response) => {
      const organisations = response.data.Organisations;
      const summarized = d3.rollups(organisations, row => row.length, row => row.organisationType)
        .map(group => new Object({ type: group[0], count: group[1] }));
      data.value = reverseSortData(summarized, "count");
      loading.value = false;
    })
    .catch((error) => {
      hasError.value = true;
      error.value = error;
    });
});
</script>
