<template>
  <Page>
    <PageHeader
      title="molgenis-viz"
      subtitle="Chart library"
      height="medium"
      class="bg-blue-050"
    />
    <PageSection :verticalPadding="2" width="large">
      <h2>Available charts</h2>
      <MessageBox type="warning">
        <p>
          No charts founds. Charts created using the
          <router-link
            :to="{ name: 'charts-builder' }"
            style="text-decoration: underline"
          >
            Chart Builder
          </router-link>
          will appear here.
        </p>
      </MessageBox>
    </PageSection>
    <Dashboard>
      <DashboardRow :columns="1">
        <DashboardChart>
          <GeoMercatorEmx
            chartId="overview-of-institutions"
            title="Location of organisations"
            description="Organisations by type in France, Belgium, the Netherlands, and Germany"
            table="Organisations"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            group="organisationType"
            :groupColorMappings="{
              Healthcare: '#f0f0f0',
              Education: 'orange',
              Facility: 'red',
            }"
            :showTooltip="true"
            :zoomLimits="[0.5, 5.0]"
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
import MessageBox from "../components/display/MessageBox.vue";

import GeoMercatorEmx from "../components/viz/GeoMercatorEmx.vue";
</script>
