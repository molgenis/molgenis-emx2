<template>
  <Page>
    <PageSection :verticalPadding="2">
      <BarChartEmx
        chartId="patients-by-recruitment-center"
        title="Number of patients submitted by research center"
        table="ClinicalData_groupBy"
        xvar="_sum { n }"
        yvar="sampleType { name }"
        y-axis-line-breaker=" "
        :chartMargins="{
          top: 10,
          right: 20,
          bottom: 60,
          left: 90
        }"
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
import MessageBox from "../components/display/MessageBox.vue";

import BarChartEmx from "../components/viz/BarChartEmx.vue";
import PieChart2Emx from "../components/viz/PieChart2Emx.vue";
import ColumnChartEmx from "../components/viz/ColumnChartEmx.vue";
import GroupedColumnChartEmx from "../components/viz/GroupedColumnChartEmx.vue";
</script>
