<script setup lang="ts">
// @ts-expect-error
import { Page, PageHeader, PageSection } from "molgenis-viz";
import Address from "../components/Address.vue";
</script>

<template>
  <Page>
    <PageHeader
      title="NESTOR Registry"
      subtitle="Contact Us"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection aria-labelledby="contact-us-section-title">
      <h2 id="contact-us-section-title">Contact Us</h2>
      <Address :showEmail="true" :centerText="true" />
    </PageSection>
  </Page>
</template>
