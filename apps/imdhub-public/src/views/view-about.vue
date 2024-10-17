<template>
  <Page>
    <PageHeader
      title="Recon4imd: IMD-hub"
      subtitle="The Inherited Metabolic Diseases database"
      imageSrc="img/recon4imd_header.jpg"
      height="medium"
    />
    <PageSection aria-labelledby="about-section-title" :vertical-padding="2">
      <h2 id="about-section-title">About Us</h2>
      <p>
        The objectives of the Reconstruction and Computational Modelling for
        Inherited Metabolic Diseases&mdash;or Recon4imd&mdash;project are-
      </p>
      <ol>
        <li>
          Classification of patients according to specific causative genetic
          defects in metabolic pathways leading to accelerated diagnosis
        </li>
        <li>
          Classification of patients according to molecular mechanistic features
          that associate with clinical phenotypic severity, enabling
          personalised disease management
        </li>
      </ol>
      <p>
        To accomplish this, patients will be recruited from participating sites
        across Europe via existing rare disease registries, networks, and
        cohorts. More details about the recruitment process and clinical
        activites are outlined in the
        <a href="https://www.recon4imd.org/clinical">
          Clinical recruitment and enrolment
        </a>
        page.
      </p>
      <p>
        For more background information, please read our
        <a href="https://www.recon4imd.org/rationale">project rationale</a> and
        <a href="https://www.recon4imd.org/objectives">project objectives</a>
        pages.
      </p>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Page, PageHeader, PageSection } from "molgenis-viz";
</script>
