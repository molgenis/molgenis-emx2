<template>
  <Page>
    <PageHeader
      title="Recon4imd: IMD-hub"
      subtitle="Participant recruitment dashboard"
      imageSrc="img/recon4imd_header.jpg"
      height="medium"
    />
    <Dashboard
      :vertical-padding="0"
      :horizontal-padding="2"
      class="bg-blue-050"
    >
      <MessageBox type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
      <DashboardRow :columns="1">
        <DashboardChart>
          <GeoMercatorEmx
            chartId="organisations-map"
            title="Recon4IMD Clinical Sites"
            description="The map below shows the location of all sites participating in the Recon4imd project."
            schema="IMDhub Refs"
            table="Organisations"
            rowId="code"
            latitude="latitude"
            longitude="longitude"
            group="name"
            :tooltipTemplate="
              (row) => {
                return `<p class='title'>${row.name}</p>`;
              }
            "
            :enableLegendClicks="true"
            :pointRadius="7"
            :mapColors="{
              land: '#709190',
              border: '#061428',
              water: '#061428',
            }"
            :mapCenter="{ latitude: 13, longitude: 50 }"
            :zoomLimits="[0.5, 10]"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import {
  Page,
  PageHeader,
  PageSection,
  MessageBox,
  Dashboard,
  DashboardRow,
  DashboardChart,
  GeoMercatorEmx,
  // @ts-ignore
} from "molgenis-viz";
</script>
