<template>
  <Page id="page-home">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="European Reference Network for rare and/or complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
      height="medium"
    />
    <PageSection aria-labelledby="welcome-section-title" :verticalPadding="2">
      <h2 id="welcome-section-title">Welcome to ERN CRANIO Registry</h2>
      <p>
        The <strong>ERN CRANIO</strong> registry is the European registry
        focused on the outcome of treatment using standardised diagnosis
        specific outcome sets (starting with craniosynostosis and cleft
        lip/palate) - including patient reported outcome measures and outcomes
        on patients' quality of life.
      </p>
    </PageSection>
    <PageSection
      aria-labelledby="quicklinks-card-title"
      width="full"
      :verticalPadding="0"
      :horizontalPadding="0"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <router-link :to="{ name: 'about' }">About</router-link>
        </LinkCard>
        <LinkCard id="quicklink-dashboard">
          <router-link :to="{ name: 'dashboard' }">Dashboard</router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection aria-labelledby="about-section-title" :verticalPadding="2">
      <h2 id="about-section-title">About the ERN CRANIO Registry</h2>
      <p>
        In 2019, ERN CRANIO responded to a call under the third programme for
        the union's action in the field of health (2014-2020) and applied for
        financial support to develop a patient registry. The network has
        successfully obtained this support to develop this registry.
      </p>
      <p>
        ERN CRANIO is developing a registry focused on the outcome of treatment
        using standardised diagnosis specific outcome sets (starting with
        craniosynostosis and cleft lip/palate) - including patient reported
        outcome measures and outcomes on patients' quality of life.
      </p>
      <p>
        To support the goals of ERN CRANIO, to pool together disease-specific
        expertise, knowledge and resources from across Europe, a European-wide
        registry is being developed for patients with rare and complex
        craniofacial anomalies, and ENT disorders. This registry allows for
        secure collection of patient data from across Europe, which will improve
        the understanding and treatment of these conditions. It will also
        facilitate research and lead to better care for these patients.
      </p>
    </PageSection>
    <PageSection aria-labelledby="contact-section-title" :verticalPadding="2">
      <h2 id="contact-section-title">Contact</h2>
      <p>
        In case of any questions regarding the ERN CRANIO registry, please email
        to:
        <a href="mailto:ern-cranio@erasmusmc.nl">ern-cranio@erasmusmc.nl</a>.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
</script>

<style lang="scss">
.quicklink-cards {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;

  .link-card {
    height: 7em;
    flex-grow: 1;

    &:nth-child(1) {
      background-color: $blue-500;
    }

    &:nth-child(2) {
      background-color: $blue-300;
    }
    &:nth-child(3) {
      background-color: $blue-100;
      a {
        color: $blue-900;
      }
    }
  }
}
</style>
