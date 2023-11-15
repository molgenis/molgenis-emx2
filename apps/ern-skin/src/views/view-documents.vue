<template>
  <Page id="page-documents">
    <PageHeader
      class="project-header main-header"
      title="ERN-Skin"
      subtitle="Download Documents"
      imageSrc="/app-header-background.png"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
    >
      <h2 id="section-documents-title">Documents</h2>
      <p>Download additional information about the ERRAS Registry.</p>
      <MessageBox class="page-warning" type="warning">
        <p>This page is under construction.</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
