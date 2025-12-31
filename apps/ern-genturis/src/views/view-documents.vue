<template>
  <Page id="page-documents">
    <PageHeader
      class="genturis-header"
      title="ERN GENTURIS registry"
      subtitle="Registry for Genetic Tumour Risk Syndromes"
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
      <h2 id="genturis-section-documents-title">Documents</h2>
      <p>
        The latest newsletter can be found
        <a
          href="https://www.genturis.eu/l=eng/news-and-events-1/newsletter.html"
          target="_blank"
          >here</a
        >
      </p>
      <p>
        More information about the GENTURIS registry can be found in the
        following documents:
      </p>
      <FileList table="Files" labelsColumn="name" fileColumn="file" />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
</script>
