<template>
  <Page id="page-home">
    <PageHeader
      title="KWF NESTOR"
      subtitle="Subtitle will go here"
      imageSrc="background_image.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection
      id="kwf-section-welcome"
      aria-labelledby="kwf-section-welcome-title"
      :verticalPadding="2"
      width="large"
    >
      <img
        src=""
        class="kwf-logo logo-medium logo-center"
        alt="KWF NESTOR logo will go here"
      />
      <h2 id="kwf-section-welcome-title">Welcome to the NESTOR Registry</h2>
      <p>
        The Netherlands Genetic Tumour Risk Registry
        <strong>NESTOR</strong> Registry is the European registry for patients
        with genetic tumour risk syndromes (genturis). It is associated with 8
        Dutch centres participating in the KWF NESTOR project and serves as a
        comprehensive resource for all patients with one of the rare genetic
        tumor risk syndromes.
      </p>
    </PageSection>
    <PageSection
      aria-labelledby="quicklinks-card-title"
      width="full"
      :verticalPadding="0"
      :horizontalPadding="0"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">
        quick links: learn more about KWF NESTOR
      </h2>
      <QuickLinks
        id="quickLinks"
        aria-labelledby="quicklinks-card-title"
        :data="[
          { name: 'about', label: 'About Us' },
          {
            name: 'documents',
            label: 'Documents',
            image: 'bkg-image-documents.jpg',
          },
          {
            name: 'dashboard',
            label: 'Dashboard',
            image: 'bkg-image-dashboard.jpg',
          },
        ]"
        name="name"
        label="label"
        imageSrc="image"
      />
    </PageSection>
    <PageSection
      id="kwf-section-aims"
      class="bg-gray-050"
      aria-labelledby="kwf-section-aims-title"
      :verticalPadding="2"
      width="large"
    >
      <h2 id="kwf-section-aims-title">Aims of KWF NESTOR</h2>
      <p>Some information will go here</p>
    </PageSection>
    <PageSection
      id="kwf-section-contact"
      aria-labelledby="kwf-section-contact-title"
      :verticalPadding="2"
      width="large"
    >
      <h2 id="kwf-section-contact-title">Contact</h2>
      <p>Contact information will go here</p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
import QuickLinks from "../components/QuickLinks.vue";
</script>
