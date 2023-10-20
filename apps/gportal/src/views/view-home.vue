<template>
  <Page>
    <PageHeader
      title="Local GDI Portal"
      subtitle="Search for data and request access"
      imageSrc="gdi-portal.jpg"
      height="large"
      titlePositionX="center"
    />
    <PageSection aria-labelledby="get-started-title" :verticalPadding="2">
      <h2 id="get-started-title">Get Started</h2>
      <p>Welcome to the <strong>GDI Local Portal</strong>. Here you can search for datasets and records, and apply for access to one or more datasets. To get started, use the dataset search for find collections of use the beacon search to find individual records.</p>
    </PageSection>
    <PageSection class="bg-gray-050" :verticalPadding="2" aria-labelledby="search-for-data-title">
      <h2 id="search-for-data-title" class="visually-hidden">Search for data</h2>
      <div class="link-card-container">
        <LinkCard>
          <router-link :to="{ name: 'datasets' }">Find Datasets</router-link>
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'beacon' }">Search with Beacon</router-link>
        </LinkCard>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
</script>