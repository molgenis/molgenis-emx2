<template>
  <Page id="page-home">
    <PageHeader
      title="NESTOR Registry"
      subtitle="Subtitle will go here"
      imageSrc="img/background_image.jpg"
      titlePositionX="center"
      titlePositionY="center"
      height="medium"
    />
    <PageSection
      id="section-welcome"
      aria-labelledby="section-welcome-title"
      :verticalPadding="2"
      width="medium"
    >
      <img
        src="/img/nestor-logo.png"
        class="logo logo-medium logo-center"
        alt="KWF NESTOR logo will go here"
      />
      <h2 id="section-welcome-title">Welcome to the NESTOR Registry</h2>
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
            image: 'img/bg-documents.jpg',
          },
          {
            name: 'dashboard',
            label: 'Dashboard',
            image: 'img/bg-dashboard.jpg',
          },
        ]"
        name="name"
        label="label"
        imageSrc="image"
      />
    </PageSection>
    <PageSection
      id="section-aims"
      class="bg-gray-050"
      aria-labelledby="section-aims-title"
      :verticalPadding="2"
      width="medium"
    >
      <h2 id="section-aims-title">Aims of the NESTOR Registry</h2>
      <p>
        There are 300,000 people in the Netherlands with a high hereditary
        cancer risk. However, there is limited information available regarding
        their risk, treatment, and prevention options. Research is further
        hindered by the inaccessibility of genetic data.
      </p>
      <p>
        Therefore the primary aim of the NESTOR Registry is to support research
        that provides valuable insights into the natural history of rare genetic
        tumor risk syndromes and the care pathway for affected patients. By
        collecting data from a large cohort of affected patients, the registry
        will enable regulated data access according to a defined data access
        policy.
      </p>
      <p>
        The registry opens up new opportunities for research, including
        translational, epidemiological studies, and clinical trials, helping to
        accelerate the generation of critical knowledge on genetic tumour risk
        syndromes. Our goal is to improve the recognition, detection,
        counselling, and treatment of patients while reducing variations in
        clinical outcomes.
      </p>
      <p>Key objectives include-</p>
      <ul>
        <li>
          Providing a sustainable, user-friendly, and interoperable web-based
          platform for standardised patient data registration from participating
          healthcare providers (HCPs) in the KWF NESTOR project, as well as
          other HCPs.
        </li>
        <li>Facilitating data sharing across the Netherlands.</li>
        <li>
          Creating a unique, comprehensive source of data from a large number of
          genturis patients or those highly suspected of having genturis.
        </li>
        <li>
          Enabling national research ranging from basic studies to clinical
          trials on genturis.
        </li>
        <li>
          Accelerating collaborative research efforts to generate new knowledge
          on rare genetic tumor risk syndromes
        </li>
      </ul>
    </PageSection>
    <PageSection
      id="section-contact"
      aria-labelledby="section-contact-title"
      :verticalPadding="2"
      width="medium"
    >
      <h2 id="section-contact-title">Contact</h2>
      <p>
        For further information, please contact the NESTOR registry coordination
        team by email
        <a href="mailto:support@molgenis.org">support@molgenis.org</a> or at the
        following address.
      </p>
      <Address :centerText="true" />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
import QuickLinks from "../components/QuickLinks.vue";
import Address from "../components/Address.vue";
</script>
