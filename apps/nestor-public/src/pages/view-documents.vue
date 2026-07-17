<script setup lang="ts">
// @ts-expect-error
import { Page, PageHeader, PageSection, FileList } from "molgenis-viz";
</script>

<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Download documents"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="documents-section-title" :verticalPadding="2">
      <h2 id="documents-section-title">Available documents</h2>
      <FileList table="Files" labelsColumn="name" fileColumn="file" />
    </PageSection>
  </Page>
</template>
