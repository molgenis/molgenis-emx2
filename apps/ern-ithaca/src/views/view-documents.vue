<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="View and download documents"
      imageSrc="img/ern-ithaca-header.jpg"
    />
    <PageSection
      id="section-temp-message"
      aria-labelledby="section-temp-message"
      :verticalPadding="2"
    >
      <h2 id="section-temp-message">Documents</h2>
      <FileList table="Files" labelsColumn="name" fileColumn="file" />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
</script>
