<template>
  <Page id="page-home">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="ERN for rare complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
      imageSrc="banner-diagnoses.jpg"
    />
    <PageSection aria-labelledby="welcome-section-title" :verticalPadding="2">
      <h2 id="welcome-section-title">Welcome to ERN CRANIO Registry</h2>
      <p>
        <strong>ERN CRANIO</strong> is establishing a European-wide registry to
        monitor treatment outcomes for rare craniofacial anomalies and ENT
        disorders like craniosynostosis and cleft lip/palate. This registry
        gathers patient data securely from across Europe, improving our
        understanding and treatment of these conditions, fostering research, and
        enhancing patient care. It covers a range of disorders and involves
        collaboration with European specialists to define relevant outcome
        measures.
      </p>
    </PageSection>
    <PageSection
      aria-labelledby="quicklinks-card-title"
      :verticalPadding="2"
      class="bg-blue-050"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <InformationCircleIcon />
          <h3>About</h3>
          <p>Learn more about the ERN Cranio registry</p>
          <router-link :to="{ name: 'about' }">Read more</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <PresentationChartLineIcon />
          <h3>Registry Dashboard</h3>
          <p>View a summary of the entire registry</p>
          <router-link :to="{ name: 'dashboard' }">View Dashboard</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <BuildingOffice2Icon />
          <h3>Healthcare Provider Login</h3>
          <p>View more information about your center</p>
          <router-link :to="{ name: 'providers' }">View Providers</router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection
      aria-labelledby="contact-section-title"
      :verticalPadding="2"
      class="section-text-center"
    >
      <h2 id="contact-section-title">Contact</h2>
      <p>
        In case of any questions regarding the ERN CRANIO registry, please send
        us an email.
        <a href="mailto:ern-cranio@erasmusmc.nl" class="button-link">
          Contact Us </a
        >.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import {
  InformationCircleIcon,
  PresentationChartLineIcon,
  BuildingOffice2Icon,
} from "@heroicons/vue/24/outline";
</script>
