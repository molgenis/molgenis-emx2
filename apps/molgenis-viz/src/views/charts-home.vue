<template>
  <Page>
    <PageHeader title="sfsf" subtitle="Chart library" />
    <PageSection :verticalPadding="2">
      <h2>Available charts</h2>
      <MessageBox type="warning">
        <p>No charts founds. Charts that you have built will appear here.</p>
      </MessageBox>
      {{ row }}
      <!-- <DataTableEmx
        tableId="data-table-test"
        table="ClinicalData_groupBy"
        columns="
          researchCenter {
            name
          }
          _sum {
            n
          }"
        :enable-row-clicks="true"
        @viz-data-clicked="(data) => row = data"
      /> -->
      <!-- <GeoMercatorEmx
        chartId="patients-by-recruitment-center"
        table="Organisations"
        rowId="code"
        latitude="latitude"
        longitude="longitude"
        group="organisationType"
        :tooltipVariables="['name', 'city', 'country']"
        :enableMarkerClicks="true"
        @viz-data-clicked="(data) => row = data"
      /> -->
      <!-- <BarChartEmx
        chartId="patients-by-recruitment-center"
        title="Number of patients submitted by research center"
        table="ClinicalData_groupBy"
        xvar="_sum { n }"
        yvar="researchCenter { name }"
        :enable-clicks="true"
        @viz-data-clicked="(data) => row = data"
      /> -->
      <!-- <ColumnChartEmx
        chartId="sample-types-chart"
        title="Overview of submitted samples"
        table="ClinicalData_groupBy"
        xvar="sampleType { name }"
        yvar="_sum { n }"
        :enable-clicks="true"
        @viz-data-clicked="(data) => row = data"
      /> -->
      <!-- <GroupedColumnChartEmx
        chartId="patients-by-recruitment-center-grouped"
        title="Number of patients submitted by research center and sex"
        table="ClinicalData_groupBy"
        xvar="sex { name }"
        group="researchCenter { name }"
        yvar="_sum { n }"
        xAxisLabel="Patients submitted"
        :yTickValues="[0,500,1000,1500,2000,2500,3000,3500,4000]"
        xAxisLineBreaker=" "        
        :chartMargins="{
          top: 20,
          right: 60,
          bottom: 80,
          left: 90
        }"
        :enable-clicks="true"
        @viz-data-clicked="(data) => row = data"
      /> -->
      <ScatterPlotEmx
        chartId="scatter-plot-demo"
        table="Organisations"
        xvar="longitude"
        yvar="latitude"
        :enable-clicks="true"
        @viz-data-clicked="(data) => (row = data)"
      />
      <!-- <PieChart2Emx
        chartId="number-of-patients-by-sex"
        title="Number of Submitted Patients by Sex"
        description="To date, all centers have submitted data."
        table="ClinicalData_groupBy"
        categories="sex { name }"
        values="_sum { n }"
        legendPosition="bottom"
      /> -->
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Page from "../components/layouts/Page.vue";
import PageHeader from "../components/layouts/PageHeader.vue";
import PageSection from "../components/layouts/PageSection.vue";
import MessageBox from "../components/display/MessageBox.vue";

import DataTableEmx from "../components/viz/DataTableEmx.vue";
import BarChartEmx from "../components/viz/BarChartEmx.vue";
import PieChart2Emx from "../components/viz/PieChart2Emx.vue";
import ColumnChartEmx from "../components/viz/ColumnChartEmx.vue";
import GroupedColumnChartEmx from "../components/viz/GroupedColumnChartEmx.vue";
import ScatterPlotEmx from "../components/viz/ScatterPlotEmx.vue";
import GeoMercatorEmx from "../components/viz/GeoMercatorEmx.vue";

const row = ref<object | null>(null);
</script>
