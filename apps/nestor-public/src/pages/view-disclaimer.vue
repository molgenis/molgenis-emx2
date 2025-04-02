<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Disclaimer Statement"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="disclaimer-section-title">
      <h2 id="disclaimer-section-title">Disclaimer statement</h2>
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
