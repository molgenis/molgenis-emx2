<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="About the NESTOR Registry"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="about-us-section-title">
      <h2 id="about-us-section-title">About the NESTOR Registry</h2>
      <p>
        The NESTOR Registry is a web-based platform that facilitates and
        accelerates research to improve diagnostics, treatment and the provision
        of high-quality healthcare for patients with one of the rare genetic
        tumour risk syndromes. The NESTOR Registry is affiliated to the KWF
        NESTOR project.
      </p>
      <MessageBox type="warning">
        <span>This page is under construction.</span>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-expect-error
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
</script>
