<template>
  <Page id="page-documents">
    <CustomPageHeader
      class="erras-header"
      title="ERN-Skin Registry"
      subtitle="Download Documents"
      imageSrc="erras-header.jpg"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
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
import { Page, PageSection, MessageBox } from "molgenis-viz";
import CustomPageHeader from "../components/CustomPageHeader.vue";
</script>
