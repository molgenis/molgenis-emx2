<template>
  <Page id="page-contact-us">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis Registry"
      subtitle="Contact Us"
      imageSrc="genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection id="genturis-contact" aria-labelledby="genturis-contact-title" :verticalPadding="2" width="large">
      <h2 id="genturis-section-contact-title">Contact</h2>
      <p>For further information, please contact the GENTURIS registry coordination team by email <a href="mailto:genturis-registry@umcg.nl">genturis-registry@umcg.nl</a> or at the following address.</p>
      <GenturisAddress :centerText="true" />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from 'molgenis-viz';
</script>