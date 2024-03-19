<template>
  <Page>
    <!-- <PageHeader
      title="MOLGENIS VIZ"
      subtitle="Create visualizations from your database"
    /> -->
    <PageSection>
      <!-- <ColumnChartEmx
        chartId="test"
        table="ClinicalData_groupBy"
        xvar="researchCenter { name }"
        yvar="_sum { n }"
      /> -->
      <GroupedColumnChartEmx
        chartId="grouped-chart-test"
        table="ClinicalData_groupBy"
        xvar="researchCenter { name }"
        yvar="_sum { n }"
        group="sex { name }"
      />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Dashboard from "../components/layouts/Dashboard.vue";
import DashboardRow from "../components/layouts/DashboardRow.vue";
import DashboardChart from "../components/layouts/DashboardChart.vue";

import BarChartEmx from "../components/viz/BarChartEmx.vue";
import ColumnChartEmx from "../components/viz/ColumnChartEmx.vue";
import GroupedColumnChartEmx from "../components/viz/GroupedColumnChartEmx.vue";
</script>
