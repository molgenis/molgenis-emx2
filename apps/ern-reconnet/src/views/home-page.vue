<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, LinkCardGroup } from "molgenis-viz";
</script>

<template>
  <Page>
    <PageHeader
      class="app-page-header"
      title="ERN ReCONNET"
      subtitle="European Reference Network on Rare and Complex Connective Tissue and Musculoskeletal Diseases"
      imageSrc="img/ern-reconnet-header.jpg"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="welcome-title">Welcome to the ERN ReCONNET registry!</h2>
      <p>
        The main objectives of the
        <strong>Registries and eHealth WG</strong> are:
      </p>
      <ul>
        <li>
          strengthen the provision of highly specialised multi-disciplinary
          cross-border virtual consultations via the Clinical Patient Management
          System (CPMS) and improve it awareness and use in ERN ReCONNET;
        </li>
        <li>
          support the implementation of the TogethERN ReCONNET registry
          infrastructure.
        </li>
      </ul>
    </PageSection>
    <PageSection
      width="full"
      aria-labelledby="quicklinks-card-title"
      :horizontalPadding="0"
      :verticalPadding="0"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <LinkCardGroup
        :linkCards="[
          { name: 'about-us', label: 'About Us' },
          {
            name: 'documents',
            label: 'Documents',
            imageSrc: 'img/ern-reconnet-docs-header.jpg',
          },
          {
            name: 'dashboard',
            label: 'Dashboard',
            imageSrc: 'img/ern-reconnet-dashboard-header.jpg',
          },
        ]"
      />
    </PageSection>
    <PageSection aria-labelledby="aims-title" :verticalPadding="2">
      <h2 id="aims-title">Aims of the ERN ReCONNET Registry</h2>
      <p>
        The main aim of ERN ReCONNET is to improve the management of rare
        connective tissue and musculo-skeletal diseases (rCTDs) across the EU.
        The main objectives of ERN ReCONNET are:
      </p>
      <ul>
        <li>
          Realisation of the potential of European cooperation among rCTDs
          stakeholders, by providing a stable and fully functioning European
          infrastructure on rCTDs;
        </li>
        <li>
          Provision of highly-specialised care for rCTDs patients and promotion
          of improvements in the cost-effective delivery of diagnosis,
          management and monitoring of rCTDs patients;
        </li>
        <li>
          Pooling, advancing and exchanging of knowledge and information on
          rCTDs by providing training and education for rCTDs stakeholders and
          by facilitating the mobility of expertise;
        </li>
        <li>
          Stimulating and encouraging collaborative patient-centered research in
          rCTDs;
        </li>
        <li>
          Promoting the empowerment and the involvement of rCTDs patients in the
          rCTDs community.
        </li>
      </ul>
    </PageSection>
  </Page>
</template>
