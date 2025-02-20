<template>
  <Page>
    <PageHeader
      title="KWF NESTOR"
      subtitle="Contact Us"
      imageSrc="background_image.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="contact-us-section-title">
      <h2 id="contact-us-section-title">Contact Us</h2>
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
