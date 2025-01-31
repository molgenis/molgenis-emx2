<template>
  <Page>
    <PageHeader
      title="ERN ReCONNET"
      subtitle="European Reference Network on Rare and Complex Connective Tissue and Musculoskeletal Diseases"
      imageSrc="img/ern-reconnet-header.jpg"
    />
    <PageSection aria-labelledby="welcome-title">
      <h2 id="welcome-title">ERN ReCONNET registry Documents</h2>
      <FileList table="Files" labelsColumn="name" fileColumn="file" />
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
</script>
