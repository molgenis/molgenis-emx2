<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="ILIAD rare diseases patient registry"
      imageSrc="ern-ithaca-header.jpg"
      height="large"
    />
    <PageSection
      id="section-welcome"
      aria-labelledby="section-welcome-title"
      :verticalPadding="2"
    >
      <h2 id="section-welcome-title">Welcome to the ILIAD registry</h2>
      <p>
        The main objective of this WP is to set up an interoperable registry
        dedicated to rare diseases within the scope of ITHACA called
        <strong>ILIAD Rare Diseases patient registry</strong>: an International
        Library of Intellectual disability and Anomalies of Development.
      </p>
    </PageSection>
    <PageSection
      id="section-quick-links"
      class="section-background-brand"
      aria-labelledby="section-quick-links-title"
      :verticalPadding="2"
    >
      <h2 id="section-quick-links-title" class="visually-hidden">
        Learn more about the registry
      </h2>
      <div class="quick-links-container ithaca-quick-links">
        <LinkCard>
          <router-link :to="{ name: 'about' }">About Us</router-link>
          <PlusIcon />
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'documents' }">Documents</router-link>
          <PlusIcon />
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
          <PlusIcon />
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'transparency' }">Transparency</router-link>
          <PlusIcon />
        </LinkCard>
      </div>
    </PageSection>
    <PageSection
      id="section-aims"
      aria-labelledby="section-aims-title"
      :verticalPadding="2"
    >
      <h2 id="section-aims-title">Aims of the ILIAD registry</h2>
      <p>
        We intend to develop a single, trans-ERN “meta-registry” (ILIAD) of
        patients with developmental anomalies (dysmorphic/Multiple Congenitital
        Anomalies syndromes and/or Iintellectual Disabilities) recruited by ERN
        ITHACA, patients with several developmental disorders affecting the head
        recruited by ERN-CRANIO, and patients with connective tissue disorders
        recruited by ERN SKIN.
      </p>
      <p>The ILIAD registry will include three types of patients.</p>
      <div class="icon-cards-container">
        <div class="icon-card">
          <div class="card-icon">
            <FingerPrintIcon />
          </div>
          <p class="card-heading">genetically defined</p>
          <p class="card-description">
            Patients have a genetic and a genomic diagnosis to be recorded
          </p>
        </div>
        <div class="icon-card">
          <div class="card-icon">
            <ClipboardDocumentListIcon />
          </div>
          <p class="card-heading">clinically defined</p>
          <p class="card-description">
            Patients have a clinical diagnosis with a disease-level ORPHA code
          </p>
        </div>
        <div class="icon-card">
          <div class="card-icon">
            <UserIcon />
          </div>
          <p class="card-heading">undiagnosed patients</p>
          <p class="card-description">
            Patients that do not have a genetic or clincial diagnosis
          </p>
        </div>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import {
  PlusIcon,
  FingerPrintIcon,
  ClipboardDocumentListIcon,
  UserIcon,
} from "@heroicons/vue/24/outline";
</script>
