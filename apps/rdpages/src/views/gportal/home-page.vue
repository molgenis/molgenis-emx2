<template>
  <Page>
    <PageHeader
      title="GDI Portal"
      subtitle="Search for data and request access"
      imageSrc="/gportal-header.jpg"
    />
    <PageSection aria-labelledby="get-started-title" :verticalPadding="2">
      <h2 id="get-started-title">Get Started</h2>
      <p>
        Welcome to the <strong>GDI Portal</strong>. In the portal, you can
        search for datasets and records to find the data of interested. If you
        would like to work with some of the data, you can submit a request of
        access on the search page.
      </p>
    </PageSection>
    <PageSection aria-labelledby="quick-links-title" width="full" :horizontal-padding="0">
      <h2 id="quick-links-title" class="visually-hidden">Quick links</h2>
      <LinkCardContainer id="gportalquicklinks">
        <LinkCard>
          <router-link :to="{ name: 'gportal-dataset' }">Datasets</router-link>
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'gportal-beacon' }">Records</router-link>
        </LinkCard>
      </LinkCardContainer>
    </PageSection>
    <PageSection aria-labelledby="about-title" :verticalPadding="2">
      <h2 id="about-title">About the Portal</h2>
      <p>more information coming soon...</p>
    </PageSection>
  </Page>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import LinkCardContainer from "../../components/LinkCardContainer.vue";
</script>

<style lang="scss">
#gportalquicklinks {
  .link-card {
    &:nth-child(1) {
      background-color: $green-200;
      a {
        color: $green-900;
      }
    }
    &:nth-child(2) {
      background-color: $green-500;
    }
  }
}
</style>
