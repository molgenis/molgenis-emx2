<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="View and download documents"
      imageSrc="ern-ithaca-header.jpg"
      height="large"
    />
    <PageSection
      id="section-temp-message"
      aria-labelledby="section-temp-message"
      :verticalPadding="2"
    >
      <h2 id="section-templ-message">Documents</h2>
      <MessageBox type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
