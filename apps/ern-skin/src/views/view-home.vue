<template>
  <Page id="page-home">
    <PageHeader
      class="project-header main-header"
      title="ERN-Skin Registry"
      subtitle="Registry for Rare and Undiagnosed Skin Diseases"
      imageSrc="/app-header-background.png"
    />
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
      class="quick-links"
      width="large"
      aria-labelledby="quicklinks-card-title"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">
        quick links: learn more about ERN Skin
      </h2>
      <QuickLinks
        id="skinQuickLinks"
        aria-labelledby="quicklinks-card-title"
        :data="[
          { name: 'about', label: 'About Us' },
          {
            name: 'documents',
            label: 'Documents',
          },
          {
            name: 'dashboard',
            label: 'Dashboard',
          },
        ]"
        name="name"
        label="label"
      />
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
        src="/ern-skin-groups.png"
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
        src="/ern-skin-erdri.png"
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
      <address class="address">
        <span><strong>Coordinating Center</strong></span>
        <span>Hôpital Necker Enfants Malades</span>
        <span>Department of Dermatology</span>
        <span>149 rue de Sèvres</span>
        <span>75015 Paris</span>
      </address>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, QuickLinks } from "molgenis-viz";
</script>
