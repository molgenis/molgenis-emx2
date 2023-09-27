<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="Our transparency statement"
      imageSrc="ern-ithaca-header.jpg"
      height="large"
    />
    <PageSection
      id="section-temp-message"
      aria-labelledby="section-temp-message-title"
      :verticalPadding="2"
    >
      <h2 id="section-temp-message-title">Our transparency statement</h2>
      <MessageBox type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
