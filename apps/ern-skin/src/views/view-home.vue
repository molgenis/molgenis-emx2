<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection, LinkCard, Address } from "molgenis-viz";
import {
  InformationCircleIcon,
  PresentationChartLineIcon,
  DocumentTextIcon,
} from "@heroicons/vue/24/outline";
</script>

<template>
  <Page id="page-home">
    <PageHeader
      imageSrc="img/erras-header.jpg"
      height="large"
      titlePositionX="center"
      titlePositionY="center"
    >
      <div class="erras-header text-center p-4">
        <h1 class="h5 m-0 text-uppercase font-weight-bold">
          ERN-Skin Registry
        </h1>
        <h2 class="h1 m-0 font-weight-light">
          Registry for Rare and Undiagnosed Skin Diseases
        </h2>
      </div>
    </PageHeader>
    <PageSection
      id="section-welcome"
      aria-labelledby="section-welcome-title"
      :verticalPadding="2"
    >
      <h2 id="section-welcome-title">Welcome to the ERN-Skin registry!</h2>
      <p>
        The general objective of this proposal is to build a central registry
        for all rare skin diseases, connected with the existing registries and
        including patients with undiagnosed cases.
      </p>
    </PageSection>
    <PageSection
      class="quick-links bg-blue-100"
      aria-labelledby="quicklinks-card-title"
      :verticalPadding="2"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">
        quick links: learn more about ERN Skin
      </h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about" class="link-card">
          <InformationCircleIcon />
          <h3>About</h3>
          <p>Learn more about the ERRAS registry</p>
          <router-link :to="{ name: 'about' }">Read more</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <PresentationChartLineIcon />
          <h3>Registry Dashboard</h3>
          <p>View a summary of the entire registry</p>
          <router-link :to="{ name: 'dashboard' }">View Dashboard</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard" class="link-card">
          <DocumentTextIcon />
          <h3>Documents</h3>
          <p>Find and download ERRAS documents</p>
          <router-link :to="{ name: 'documents' }">View Documents</router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection
      id="section-aims"
      class="bg-gray-050"
      aria-lablledby="section-aims-title"
      :verticalPadding="2"
    >
      <h2 id="section-aims-title">Aims of the ERN-Skin registry</h2>
      <p>
        The <strong>ERN-Skin</strong> gathers European medical experts and
        patient representatives who join their efforts to improve the diagnosis
        and management of all the patients suffering from rare, complex and
        undiagnosed skin diseases all over Europe.
      </p>
      <p>
        Structured in 9 Disease Groups and Transversal thematic Groups, ERN-Skin
        has been developing an innovative e-health platform allowing the
        construction of a common Skin Diseases registry: ERRAS, a key instrument
        for our ERN.
      </p>
      <!-- insert image: ern-skin box diagram -->
      <img
        src="/img/ern-skin-groups.png"
        class="project-diagrams"
        alt="ERN-Skin disease groups"
      />
      <p>
        The SKIN ePAGs bring together rare skin disease patient representatives
        who are actively involved in the work of the ERN-Skin.
      </p>
      <p>
        ERRAS, the European registry for rare dermatologic diseases receives
        information from existing European registries as well as additional
        manual feeds. It is a means to put scientists and patients together via
        their respective healthcare providers.
      </p>
      <!-- insert image: ERDI image -->
      <img
        src="/img/ern-skin-erdri.png"
        class="project-diagrams"
        alt="European registry for rare dermatologic diseases"
      />
    </PageSection>
    <PageSection
      id="section-contact"
      aria-labelledby="section-contact-title"
      :verticalPadding="2"
    >
      <h2 id="section-contact-title">Contact</h2>
      <p>
        If you want to know more, to participate, or to consult the ERN-Skin
        registry, thanks to send your query to the following email address:
        <a href="mailto:skin-registry@ern-skin.eu>">
          skin-registry@ern-skin.eu
        </a>
        or at the following address.
      </p>
      <Address
        name="Coordinating Center"
        :address="[
          'Hôpital Necker Enfants Malades',
          'Department of Dermatology',
          '149 rue de Sèvres',
          '75015 Paris',
        ]"
      />
    </PageSection>
  </Page>
</template>
