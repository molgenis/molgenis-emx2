<template>
  <Page id="page-documents">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis Registry"
      subtitle="Download documents"
      imageSrc="genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection
      id="genturis-section-documents"
      aria-labelledby="genturis-section-documents-title"
      class="bg-gray-050"
      :verticalPadding="2"
    >
      <h2 id="genturis-section-documents-title">Documents</h2>
      <p>Download additional information about the GENTURIS Registry.</p>
      <FileList
        table="Files"
        filename="name"
        path="path"
      />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import FileList from "../components/FileList.vue";
</script>
