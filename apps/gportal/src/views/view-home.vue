<template>
  <Page>
    <PageHeader
      title="Local GDI Portal"
      subtitle="Search for data and request access"
      imageSrc="gdi-portal.jpg"
      height="large"
      titlePositionX="center"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="get-started-title">Welcome to the GDI Local Portal</h2>
      <p>
        The GDI Local Portal serves a primary repository for the metadata
        currated by an organisation. The portal gives data managers the ability
        to upload metadata using a interface or the import batches of data using
        API calls. Data managers can also modify the existing metadata and
        create collections of individual records in a meaningful way that makes
        data findable and accessibile.
      </p>
      <p>
        To get started, use the dataset search for find collections or use the
        beacon search to find individual records. If you have data that you
        would like to add to the collection or manage the local portal, then
        have a look at the
        <router-link :to="{ name: 'get-started' }">
          Get Started Guide </router-link
        >.
      </p>
    </PageSection>
    <PageSection
      class="bg-primary-alt"
      :verticalPadding="2"
      aria-labelledby="search-for-data-title"
    >
      <h2 id="search-for-data-title" class="visually-hidden">
        Search for data
      </h2>
      <div class="link-card-container">
        <LinkCard imageSrc="bkg-datasets.jpg">
          <router-link :to="{ name: 'datasets' }">
            Find Datasets
            <ArrowRightCircleIcon />
          </router-link>
        </LinkCard>
        <LinkCard imageSrc="bkg-beacon.jpg">
          <router-link :to="{ name: 'beacon' }">
            Search with Beacon
            <ArrowRightCircleIcon />
          </router-link>
        </LinkCard>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import { ArrowRightCircleIcon } from "@heroicons/vue/24/outline";
</script>
