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
      width="large"
    >
      <h2 id="genturis-section-documents-title">Documents</h2>
      <p>Download additional information about the GENTURIS Registry.</p>
      <MessageBox type="error" v-if="error">
        <p>
          Unable to retrieve the list of available files. Please sign in or try
          again later.
        </p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
