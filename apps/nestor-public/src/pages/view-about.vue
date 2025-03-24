<template>
  <Page>
    <PageHeader
      title="KWF NESTOR"
      subtitle="About Us"
      imageSrc="background_image.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="about-us-section-title">
      <h2 id="about-us-section-title">About Us</h2>
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
