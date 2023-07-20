<template>
  <Page>
    <PageHeader
      title="ERN ReCONNET"
      subtitle="European Reference Network on Rare and Complex Connective Tissue and Musculoskeletal Diseases"
      :imageSrc="ProjectBannerImage"
    />
    <PageSection aria-labelledby="welcome-title">
      <h2 id="welcome-title">ERN ReCONNET registry dashboard</h2>
      <MessageBox>
        <p>This page is under construction.</p>
      </MessageBox>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import ProjectBannerImage from "../assets/ern-reconnet-header.jpg";
</script>