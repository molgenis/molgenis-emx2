<template>
  <Page>
    <PageHeader
      class="app-page-header"
      title="ERN ReCONNET"
      subtitle="European Reference Network on Rare and Complex Connective Tissue and Musculoskeletal Diseases"
      :imageSrc="ProjectBannerImage"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="welcome-title">Welcome to the ERN ReCONNET registry!</h2>
      <p>The main objectives of the <strong>Registries and eHealth WG</strong> are:</p>
      <ul>
        <li>strengthen the provision of highly specialised multi-disciplinary cross-border virtual consultations via the Clinical Patient Management System (CPMS) and improve it awareness and use in ERN ReCONNET;</li>
        <li>support the implementation of the TogethERN ReCONNET registry infrastructure.</li>
      </ul>
    </PageSection>
    <PageSection class="quick-links" width="full" aria-labelledby="quicklinks-card-title" :horizontalPadding="0" :verticalPadding="0">
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about" :imageSrc="AboutImage">
          <router-link :to="{ name: 'about-us' }">About Us</router-link>
        </LinkCard>
        <LinkCard id="quicklink-documents" :imageSrc="DocumentsImage">
          <router-link :to="{ name: 'documents' }">Documents</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </LinkCard>
        <LinkCard id="quicklink-signin">
          <a href="#">Sign in</a>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection aria-labelledby="aims-title" :verticalPadding=2>
      <h2 id="aims-title">Aims of the ERN ReCONNET Registry</h2>
      <p>The main aim of ERN ReCONNET is to improve the management of rare connective tissue and musculo-skeletal diseases (rCTDs) across the EU. The main objectives of ERN ReCONNET are:</p>
      <ul>
        <li>Realisation of the potential of European cooperation among rCTDs stakeholders, by providing a stable and fully functioning European infrastructure on rCTDs;</li>
        <li>Provision of highly-specialised care for rCTDs patients and promotion of improvements in the cost-effective delivery of diagnosis, management and monitoring of rCTDs patients;</li>
        <li>Pooling, advancing and exchanging of knowledge and information on rCTDs by providing training and education for rCTDs stakeholders and by facilitating the mobility of expertise;</li>
        <li>Stimulating and encouraging collaborative patient-centered research in rCTDs;</li>
        <li>Promoting the empowerment and the involvement of rCTDs patients in the rCTDs community.</li>
      </ul>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import ProjectBannerImage from "../assets/ern-reconnet-header.jpg";
import AboutImage from "../assets/ern-reconnet-about-header.jpg";
import DocumentsImage from "../assets/ern-reconnet-docs-header.jpg";

</script>

<style lang="scss">
.quick-links {
  background-color: $gray-100;

  .quicklink-cards {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    
    .link-card {
      height: 8em;
      flex-grow: 1;
      
      &:nth-child(1) {
        .card-background-filter {
          background-color: $blue-800;
          opacity: 0.6;
        }
      }

      &:nth-child(2) {
        .card-background-filter {
          background-color: $blue-500;
          opacity: 0.6;
        }
      }
      
      &:nth-child(3) {
        background-color: $blue-500;
      }
      
      &:nth-child(4) {
        background-color: $blue-200;
        a {
          color: $blue-900;
        }
      }
    }    
  }
}
</style>