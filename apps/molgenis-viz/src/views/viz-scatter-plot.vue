<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Scatter Plot"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li><router-link :to="{name: 'scatter-plot'}">Scatter Plot</router-link></li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Scatter Plot</h2>
      <p>The <strong>scatter plot</strong> is ideal for displaying to continuous variables against each other. Support for grouping points is available by specifying the group to color mappings and with the legend (that's automatically generated).</p>
      <p>The example below demonstrates the scatter plot component and the grouping features.</p>
    </PageSection>
    <PageSection class="viz-section-dark" :verticalPadding="2">
      <MessageBox v-if="loading & !hasError">
        <p>Fetching data</p>
      </MessageBox>
      <MessageBox v-else-if="!loading && hasError" type="error">
        <p>{{ error }}</p>
      </MessageBox>
      <ScatterPlot 
        chartId="penguinSizeChart"
        title="Body mass and flipper length"
        :description="`The body mass (g) and flipper length (mm) are reported for ${data.length} observed penguins`"
        :chartData="data"
        group="organisationType"
        xvar="longitude"
        yvar="latitude"
        xAxisLabel="Body Mass (g)"
        yAxisLabel="Flipper Length (mm)"
        :chartMargins="{
          top: 15,
          right: 25,
          bottom: 60,
          left: 60
        }"
        :enableHover="false"
        v-else
      />
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";

import Page from '../components/layouts/Page.vue'
import PageHeader from '../components/layouts/PageHeader.vue'
import PageSection from '../components/layouts/PageSection.vue'
import MessageBox from '../components/display/MessageBox.vue'
import ScatterPlot from '../components/viz/ScatterPlot.vue'
import Breadcrumbs from '../app-components/breadcrumbs.vue'
import headerImage from "../assets/scatter-plot-header.jpg"

import { fetchData } from '../utils/utils.js' 

let loading = ref(true);
let error = ref(false);
let hasError = ref(false);
let data = ref([]);
let clicked = ref({});

const query = `{
  Organisations (
    filter: { country: { equals: "Netherlands" } } 
  ) {
    name
    latitude
    longitude
    organisationType
  }
}`

onMounted(() => {
  Promise.resolve(
    fetchData('/api/graphql', query)
  ).then(response => {
    data.value = response.data.Organisations
    loading.value = false
  }).catch(error => {
    const err = error.message
    loading.value = false
    hasError.value = true
    error.value = err
    throw new Error(error)
  })
})

</script>

<style lang="scss">
.viz-section-dark {
  background-color: $gray-700;
  color: $gray-000;
}
</style>