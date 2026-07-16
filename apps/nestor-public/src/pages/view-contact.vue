<script setup lang="ts">
// @ts-expect-error
import { Address, Page, PageHeader, PageSection } from "molgenis-viz";
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
      <Address
        name="University Medical Centre Groningen"
        email="support@molgenis.org"
        :centerText="true"
        :showEmail="true"
        :address="[
          'NESTOR registry',
          'Department of Medical Genetics',
          'Antonius Deusinglaan 1',
          '9713 AV Groningen',
          'The Netherlands',
        ]"
      />
    </PageSection>
  </Page>
</template>
