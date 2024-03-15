<template>
  <Page>
    <!-- <PageHeader
      title="MOLGENIS VIZ"
      subtitle="Create visualizations from your database"
    /> -->
    <Dashboard>
      <DashboardRow :columns="2">
        <DashboardChart>
          <BarChartEmx
            chartId="patients-by-research-center"
            title="Submitted patients by research center"
            description="To date, research centers have submitted a 10k+ patients. The chart below shows how many patients were submitted by research center."
            table="ClinicalData_groupBy"
            xvar="_sum { n }"
            yvar="researchCenter { name }"
            yAxisLineBreaker=" "
            :chartMargins="{
              top: 15,
              right: 30,
              bottom: 60,
              left: 120,
            }"
            xAxisLabel="Total number of patients"
            yAxisLabel="Research Center"
          />
        </DashboardChart>
        <DashboardChart>
          <ColumnChartEmx
            chartId="total-patients-by-sample-type"
            title="Count of patients by sample type"
            description="Research centers have submitted a number of samples to the database. The chart shows the breakdown by sample type"
            table="ClinicalData_groupBy"
            xvar="sampleType { name }"
            yvar="_sum { n }"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
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
</script>
