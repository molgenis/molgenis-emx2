<template>
  <Page id="page-documents">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis Registry"
      imageSrc="img/genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection
      id="genturis-section-documents"
      aria-labelledby="genturis-section-documents-title"
      class="bg-gray-050"
      :verticalPadding="2"
      width="large"
    >
      <h2 id="genturis-section-documents-title">Download Documents</h2>
      <p>Download additional information about the GENTURIS Registry.</p>
      <FileList table="Files" labelsColumn="name" fileColumn="file" />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
</script>
