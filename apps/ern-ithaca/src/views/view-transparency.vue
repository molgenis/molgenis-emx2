<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="Transparency statement"
      imageSrc="img/ern-ithaca-header.jpg"
    />
    <PageSection
      id="section-temp-message"
      aria-labelledby="section-temp-message-title"
      :verticalPadding="2"
    >
      <h2 id="section-temp-message-title">Transparency statement</h2>
      <MessageBox type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
