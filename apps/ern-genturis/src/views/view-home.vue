<template>
  <Page id="page-home">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis Registry"
      subtitle="Registry for Genetic Tumour Risk Syndromes"
      :imageSrc="GenturisHeaderPhoto"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection id="genturis-section-welcome" aria-labelledby="genturis-section-welcome-title" :verticalPadding="2" width="large">
      <img
        :src="GenturisLogo"
        class="genturis-logo-large"
        alt="ERN Genturis Registry - Registry for Genetic Tumour Risk Syndromes"
      />
      <h2 id="genturis-section-welcome-title">Welcome to the GENTURIS registry</h2>
      <p>The <strong>GENTURIS</strong> registry is the European registry for patients with one of the genetic tumour risk syndromes (genturis). The registry is affiliated to the European Reference Network for all patients with one of the genetic tumour risk syndromes (ERN GENTURIS).</p>
    </PageSection>
    <PageSection aria-labelledby="quicklinks-card-title" width="full" :verticalPadding="0" :horizontalPadding="0">
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <router-link :to="{ name: 'about-us' }">About Us</router-link>
        </LinkCard>
        <LinkCard id="quicklink-documents" :imageSrc="BkgPhotoDocument">
          <router-link :to="{ name: 'documents' }">Documents</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard" :imageSrc="BkgPhotoDashboard">
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </LinkCard>
        <LinkCard id="quicklink-signin">
          <a href="/login">Sign in</a>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection id="genturis-section-aims" aria-labelledby="genturis-section-aims-title" :verticalPadding="2" width="large">
      <h2 id="genturis-section-aims-title">Aims of the GENTURIS registry</h2>
      <p>The aim of the GENTURIS registry is to enable research that will provide insights into the natural disease history and care pathway by gathering data from a large number of genturis patients and providing regulated data access following a data access policy.</p>
      <p>The registry enables a new wave of research opportunities from translational to epidemiological studies to clinical trials, and will accelerate the generation of new knowledge on genetic tumour risk syndromes. Research to improve recognition, detection, counselling and treatment of patients and to reduce variation in clinical outcomes across patients.</p>
      <ul>
        <li>To provide a sustainable, user-friendly and interoperable web-based registry platform that facilitates standardised registration of data on patients with a genturis known at participating Full Member and Affiliated Partner Health Care Providers (HCPs) of the ERN GENTURIS as well as other HCPs.</li>
        <li>To facilitate <strong>sharing of data</strong> across Europe.</li>
        <li>To be a <strong>unique source of data</strong> of a large number of patients with a genturis or patients highly suspected of a genturis.</li>
        <li>To enable <strong>European research</strong> ranging from basic research to clinical trials on genturis.</li>
        <li>To accelerate the generation of <strong>new knowledge</strong> on genturis through collaborative research.</li>
      </ul>
    </PageSection>
    <PageSection id="genturis-section-contact" aria-labelledby="genturis-section-contact-title" :verticalPadding="2" width="large">
      <h2 id="genturis-section-contact-title">Contact</h2>
      <p>For further information, please contact the GENTURIS registry coordination team by email <a href="mailto:genturis-registry@radboudumc.nl">genturis-registry@radboudumc.nl</a> or at the following address.</p>
      <Address :centerText="true" />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import Address from "../components/Address.vue";
</script>