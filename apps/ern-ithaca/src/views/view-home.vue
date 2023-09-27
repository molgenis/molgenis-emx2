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
      <p>ILIAD will register 3 types of patients:</p>
      <ul>
        <li>
          genetically defined patients: patients must have a genetic/genomic
          diagnosis to be recorded
        </li>
        <li>
          clinically defined patients: patients must have a precise clinical
          diagnosis with a disease-level ORPHA code
        </li>
        <li>undiagnosed patients</li>
      </ul>
      <p>
        For more information on the structure of the registry, please see the
        <a href="#">Model Document</a>.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import { PlusIcon } from "@heroicons/vue/24/outline";
</script>
