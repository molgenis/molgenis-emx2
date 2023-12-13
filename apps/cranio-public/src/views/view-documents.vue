<template>
  <Page id="page-documents">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Documents"
      imageSrc="banner-diagnoses.jpg"
    />
    <Breadcrumbs />
    <PageSection
      id="section-documents"
      class="bg-blue-050"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
      :horizontalPadding="0"
    >
      <h2 id="section-documents-title">Download Documents</h2>
      <p>Download additional information about the CRANIO Registry.</p>
      <FileList table="Files" filename="name" path="path" />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
import Breadcrumbs from "../components/breadcrumbs.vue";
</script>
