<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import {
  PlusIcon,
  FingerPrintIcon,
  ClipboardDocumentListIcon,
} from "@heroicons/vue/24/outline";
</script>

<template>
  <Page>
    <PageHeader
      title="ERN Ithaca"
      subtitle="ILIAD rare diseases patient registry"
      imageSrc="img/ern-ithaca-header.jpg"
      height="large"
    />
    <PageSection
      id="section-welcome"
      aria-labelledby="section-welcome-title"
      :verticalPadding="2"
    >
      <h2 id="section-welcome-title">Welcome to the ILIAD registry</h2>
      <p>
        Through ERN-ITHACA’s expert and patient participation network, ILIAD
        provides an infrastructure for diagnosis, highly specialised
        multidisciplinary healthcare, evidence-based management, and collection
        of secure patient data.
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
          <a href="tables/#/Subject">Register patient </a>
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'documents' }">Documents</router-link>
          <PlusIcon />
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
          <PlusIcon />
        </LinkCard>
      </div>
    </PageSection>
    <PageSection
      id="section-illiad-info"
      aria-labelledby="section-illiad-info-title"
      :verticalPadding="2"
    >
      <h2 id="section-illiad-info-title">About the ILIAD registry</h2>
      <p>ILIAD consists of two components:</p>

      <ol>
        <li><strong>central, web-based registry, </strong></li>

        <li>
          <strong
            >a network of linked satellite/client registries forming the
            ERN-ITHACA registry federation</strong
          >,
        </li>
      </ol>

      <p>
        in databases built according to international interoperability standards
        from JRC and EJP-RD.&nbsp;
      </p>

      <p>
        In addition to the core registry, ILIAD includes
        <strong>thematic sub-registries of patients</strong> with proven
        monogenic or genomic (chromosomal) diagnoses, curated by ERN
        teams.&nbsp;
      </p>

      <p>
        <strong
          >Based on the <strong>open-source software</strong> MOLGENIS</strong
        >, it provides flexible rich data structures, user-friendly data import
        and querying, and a FAIR interface for database exchange. For
        interoperability, the registry is connected to the European Rare Disease
        Registry Infrastructure (ERDRI), uses the JRC’s common data elements and
        common pseudonymisation tools to link rare disease patient
        cohorts.&nbsp;
      </p>

      <p>
        ILIAD has <strong>a strict data access policy</strong> for requesting
        access to the data, governance of the registries, compliance with legal
        and regulatory requirements on the use of personal data.&nbsp;
      </p>

      <p>
        <strong>ERN-ITHACA patient data</strong> will be analysed to produce
        high-quality epidemiological insights and expert consensus statements,
        informing policy decisions that benefit ERN-ITHACA patients and all rare
        disease patients in general (this action is co-funded by the EU 3rd
        Health Programme: Grant number 947617).
      </p>
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
      <p>The ILIAD registry will include two types of patients.</p>
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
      </div>
    </PageSection>
  </Page>
</template>
