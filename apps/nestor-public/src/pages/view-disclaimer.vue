<script setup lang="ts">
// @ts-expect-error
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>

<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Disclaimer Statement"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection
      aria-labelledby="disclaimer-section-title"
      :verticalPadding="2"
    >
      <h2 id="disclaimer-section-title">Disclaimer statement</h2>
      <MessageBox type="warning">
        <span>This page is under construction.</span>
      </MessageBox>
    </PageSection>
  </Page>
</template>
