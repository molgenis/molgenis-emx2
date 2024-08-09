<template>
  <Page>
    <PageHeader
      title="KWF NESTOR"
      subtitle="Dashboard"
      imageSrc="bkg-image-dashboard.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="dashboard-section-title">
      <h2 id="dashboard-section-title">Dashboard</h2>
      <MessageBox type="warning">
        <span>This page is under construction.</span>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-expect-error
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
