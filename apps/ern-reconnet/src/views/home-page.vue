<template>
  <Page>
    <PageHeader
      title="ERN ReCONNET"
      subtitle="European Reference Network on Rare and Complex Connective Tissue and Musculoskeletal Diseases"
      :imageSrc="ProjectBannerImage"
    />
    <PageSection aria-labelledby="welcome-title">
      <h2 id="welcome-title">Welcome to the ERN ReCONNET registry!</h2>
      <p>The main objectives of the <strong>Registries and eHealth WG</strong> are:</p>
      <ul>
        <li>strengthen the provision of highly specialised multi-disciplinary cross-border virtual consultations via the Clinical Patient Management System (CPMS) and improve it awareness and use in ERN ReCONNET;</li>
        <li>support the implementation of the TogethERN ReCONNET registry infrastructure.</li>
      </ul>
    </PageSection>
    <PageSection class="quick-links" width="large" aria-labelledby="quicklinks-card-title">
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <ul class="quick-links-nav">
        <li>
          <router-link :to="{ name: 'about-us' }">About Us</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'documents' }">Documents</router-link>
        </li>
        <li> 
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </li>
        <li id="quicklink-signin">
          <a href="/login">Sign in</a>
        </li>
      </ul>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
import ProjectBannerImage from "../assets/ern-reconnet-header.jpg";
</script>