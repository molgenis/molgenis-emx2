<template>
  <Page>
    <!-- <PageHeader title="Home" subtitle="Some title" /> -->
    <PageSection>
      <BarChartEmx
        chartId="barChartExample"
        title="Patient recruitment by research center"
        description="To date, research centers have submitted a 10k+ patients. The chart below shows how many patients were submitted by research center."
        table="ClinicalData_groupBy"
        xvar="_sum { n }"
        yvar="researchCenter { name }"
        :xMax="10000"
        :xTickValues="[0,2500,5000,7500,10000]"
        :chartMargins="{
          top: 15,
          right: 30,
          bottom: 60,
          left: 120
        }"
        xAxisLabel="Total number of patients"
        yAxisLabel="Research Center"
      />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";

import BarChartEmx from "../components/viz/BarChartEmx.vue";
</script>
