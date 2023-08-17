<template>
  <Page>
    <PageHeader
      title="RD-Components"
      subtitle="Gauge Chart Example"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'gauge-chart' }">Gauge Chart</router-link>
        </li>
      </Breadcrumbs>
    </PageSection>
    <PageSection>
      <h2>Gauge Chart</h2>
      <p>
        The <strong>Gauge</strong> component is useful for displaying the
        progress of one numeric value in relation to the whole group. This may
        be useful for showing progress towards reaching patient recruiment
        targets, for example. The input value must be a decimal between 0 and 1,
        and the percentage should be calculated passing into the component. The
        percentage is displayed in the center of the chart.
      </p>
      <p>
        The chart respresents a scale between 0 and 100. The input value is
        always drawn clockwise starting from the 12:00 position, and it is
        always the darker colored arc. The lighter arc shows the entire amount
        (i.e., 100%). The colors can be modified, but the value arc should
        always be darker than the background arc.
      </p>
      <p>
        The example gauge charts below show the current patient recruitment
        status per group in relation to the overall recruitment goal.
      </p>
    </PageSection>
    <PageSection class="viz-section bkg-light" :verticalPadding="2">
      <h2>Gauge Chart Example</h2>
      <p>Patient recruitment by group</p>
      <div class="d3-viz-flex">
        <GaugeChart
          :chartId="row.id"
          :title="row.group"
          :value="row.percent"
          v-for="row in data"
          :key="row.group"
          :enableClicks="false"
        />
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import GaugeChart from "../components/viz/GaugeChart.vue";

import headerImage from "../assets/gauge-chart-header.jpg";

let data = ref([]);

onMounted(() => {
  data.value = ["Control", "Experimental"].map((group) => {
    const value = Math.random();
    return {
      id: `Gauge${group}`,
      group: group,
      percent: value,
    };
  });
});
</script>

<style lang="scss">
.viz-section {
  text-align: center;
}

.d3-viz-flex {
  display: grid;
  grid-template-columns: 1fr 1fr;

  .d3-gauge {
    h3.chart-title {
      text-align: center;
    }
  }
}
</style>
