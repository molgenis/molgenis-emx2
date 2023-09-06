<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Progress Charts"
      :imageSrc="headerImage"
      height="large"
    />
    <PageSection :verticalPadding="0">
      <Breadcrumbs>
        <li>
          <router-link :to="{ name: 'progress-charts' }">Progress Charts</router-link>
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
    <PageSection>
      <h2>Progress Meter</h2>
      <p><strong>Gauge Charts</strong> can be useful, but it is better to simplify the display visualisation. The <strong>ProgressMeter</strong> is a better way to display how much progress has been made. The outline shows the total possible value, and the filled in region shows how much progress has been made.</p>
      <ProgressMeter
        chartId="progressBar"
        title="Cases Completed"
        :value="32"
        :totalValue="81"
        :barHeight="30"
      />
    </PageSection>
  </Page>
</template>

<script setup>
import { ref } from "vue";

import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Breadcrumbs from "../app-components/breadcrumbs.vue";
import GaugeChart from "../components/viz/GaugeChart.vue";
import ProgressMeter from "../components/viz/ProgressMeter.vue";

import headerImage from "../assets/gauge-chart-header.jpg";

let data = ref([]);
data.value = ["Control", "Experimental"].map((group) => {
  const value = Math.random();
  return {
    id: `Gauge${group}`,
    group: group,
    percent: value,
    value: Math.round(value * 100)
  };
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
