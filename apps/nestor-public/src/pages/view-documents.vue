<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Download documents"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="documents-section-title">
      <h2 id="documents-section-title">Available documents</h2>
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
