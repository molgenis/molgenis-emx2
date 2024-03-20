<template>
  <Page>
    <PageHeader
      title="MOLGENIS VIZ"
      subtitle="Create visualizations from your database"
      height="medium"
      class="bg-gray-050"
    />
    <PageSection aria-labelledby="available-charts-title" :verticalPadding="2">
      <h2 id="available-charts-title">Available charts</h2>
      <MessageBox type="warning">
        <p>No charts found.</p>
      </MessageBox>
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
import ColumnChartEmx from "../components/viz/ColumnChartEmx.vue";
import GroupedColumnChartEmx from "../components/viz/GroupedColumnChartEmx.vue";
</script>
