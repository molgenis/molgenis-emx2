<template>
  <Page>
    <PageHeader
      class="app-page-header"
      title="FairDataHub"
      subtitle="FAIR DataHub Tools"
      :imageSrc="ProjectBannerImage"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="welcome-title">Welcome to the FairDataHub data manager</h2>
      <p>The purpose of this site is to interact with the data model and add new records to the ontology tables. To get started, follow one of the links below.</p>
    </PageSection>
    <PageSection
      class="quick-links"
      width="full"
      aria-labelledby="quicklinks-card-title"
      :horizontalPadding="0"
      :verticalPadding="0"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <div class="quicklink-cards">
        <LinkCard id="quicklinks-about">
          <router-link :to="{ name: 'organisations' }">Organisations</router-link>
        </LinkCard>
      </div>
    </PageSection>
    
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import ProjectBannerImage from "../assets/app-page-header.jpg";
</script>

<style lang="scss">
.quick-links {
  background-color: $gray-100;
  
  .toggle {
    background: none;
    border: none;
    color: $blue-900;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    font-weight: bold;
    font-size: 14pt;
  }

  .quicklink-cards {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;

    .link-card {
      height: 10em;
      flex-grow: 1;

      &:nth-child(1) {
        .card-background-filter {
          background-color: $blue-800;
          opacity: 0.6;
        }
      }

      &:nth-child(2) {
        .card-background-filter {
          background-color: $blue-500;
          opacity: 0.6;
        }
      }

      &:nth-child(3) {
        .card-background-filter {
          background-color: $blue-green-800;
          opacity: 0.6;
        }
      }
    }
  }
}
</style>
