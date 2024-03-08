<template>
  <Page>
    <PageHeader title="Home" subtitle="Some title" />
    <BarChartEmx
      chartId="barChartExample"
      table="ClinicalData_groupBy"
      xvar="researchCenter"
      yvar="_sum"
    />
  </Page>
</template>

<script setup lang="ts">
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";

import BarChartEmx from "../components/viz/BarChartEmx.vue";
</script>
