<template>
  <Page>
    <PageHeader
      title="Recon4imd: IMD-hub"
      subtitle="The Inherited Metabolic Diseases database"
      imageSrc="img/recon4imd_header.jpg"
      height="medium"
    />
    <PageSection :vertical-padding="2" aria-labelledby="welcome-section-title">
      <h2 id="welcome-section-title">Welcome to the IMD-hub</h2>
      <p>
        The <strong>IMD-hub</strong> aims to accelerate diagnosis and
        personalise management of inherited metabolic diseases by developing
        clinically validated decision support tools enabling accelerated
        diagnosis and personalised management of inherited metabolic diseases,
        based on genomic, proteomic, and metabolomic data-driven computational
        models.
      </p>
    </PageSection>
    <PageSection
      aria-labelledby="quicklinks-card-title"
      :verticalPadding="2"
      width="large"
      class="bg-blue-050"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <InformationCircleIcon />
          <h3>About</h3>
          <p>Learn more about the Recon4imd project</p>
          <router-link :to="{ name: 'about' }">Read more</router-link>
        </LinkCard>
        <LinkCard id="quicklink-documents">
          <DocumentTextIcon />
          <h3>Documents</h3>
          <p>Download project documents and forms</p>
          <router-link :to="{ name: 'documents' }">
            View Documents
          </router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <PresentationChartLineIcon />
          <h3>Dashboard</h3>
          <p>View a summary of the entire database</p>
          <router-link :to="{ name: 'dashboard' }">
            View Dashboard
          </router-link>
        </LinkCard>
        <LinkCard id="quicklink-request-access">
          <ShieldCheckIcon />
          <h3>Request access</h3>
          <p>Learn more about the data access request.</p>
          <router-link :to="{ name: 'request-access' }">
            Request access
          </router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection aria-labelledby="contact-section-title" :verticalPadding="2">
      <h2 id="contact-section-title">Contact</h2>
      <p>
        We are currently in the process of establishing a patient helpdesk.
        Please feel free to contact the Recon4IMD coordination office with any
        queries or suggestions that you may have.
      </p>
      <a href="mailto:info.recon4imd@gmail.com" class="button-link">
        Contact Us
      </a>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import {
  DocumentTextIcon,
  InformationCircleIcon,
  PresentationChartLineIcon,
  BuildingOffice2Icon,
  ShieldCheckIcon,
} from "@heroicons/vue/24/outline";

import type { RouterViewIF } from "../interfaces";

const props = defineProps<RouterViewIF>();
</script>
