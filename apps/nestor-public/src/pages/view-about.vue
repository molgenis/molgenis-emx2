<template>
  <Page>
    <PageHeader
      title="NESTOR Resgistry"
      subtitle="About the NESTOR registry"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="about-us-section-title">
      <h2 id="about-us-section-title">About the NESTOR registry</h2>
      <p>
        The NESTOR registry is a web-based platform that facilitates and
        accelerates research to improve diagnostics, treatment and the provision
        of high-quality healthcare for patients with one of rare genetic tumor
        risk syndromes. The NESTOR registry is affiliated to the KWF NESTOR
        project.
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
