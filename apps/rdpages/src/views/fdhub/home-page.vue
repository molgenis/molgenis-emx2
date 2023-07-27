<template>
  <Page>
    <PageHeader
      class="app-page-header"
      title="FairDataHub"
      subtitle="FAIR DataHub Tools"
      imageSrc="/fdhub-header.jpg"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="welcome-title">Welcome to the FairDataHub data manager</h2>
      <p>
        The purpose of this site is to interact with the data model and add new
        records to the ontology tables. To get started, follow one of the links
        below.
      </p>
    </PageSection>
    <PageSection
      class="quick-links"
      width="full"
      aria-labelledby="quicklinks-card-title"
      :horizontalPadding="0"
      :verticalPadding="0"
    >
      <h2 class="visually-hidden" id="quicklinks-card-title">quick links</h2>
      <LinkCardContainer id="fdhublinks">
        <LinkCard>
          <router-link :to="{ name: 'fdhub-organisations' }">
            Organisations
          </router-link>
        </LinkCard>
        <LinkCard>
          <span>HPO</span>
        </LinkCard>
        <LinkCard>
          <span>Diseases</span>
        </LinkCard>
      </LinkCardContainer>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import LinkCardContainer from "../../components/LinkCardContainer.vue";
</script>

<style lang="scss">
#fdhublinks {
  .link-card {
    &:nth-child(1) {
      background-color: $blue-800;
    }
    &:nth-child(2) {
      background-color: $blue-500;
      color: $blue-050;
    }
    &:nth-child(3) {
      background-color: $blue-200;
      color: $blue-800;
    }
  }
}
</style>
