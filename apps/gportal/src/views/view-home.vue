<template>
  <Page>
    <PageHeader
      title="Local GDI Portal"
      subtitle="Search for data and request access"
      imageSrc="gdi-portal.jpg"
      height="large"
      titlePositionX="center"
    />
    <PageSection class="bg-gray-050" :verticalPadding="2">
      <div class="portal-highlights">
        <div class="highlight">
          <data class="data" :value="highlights.Catalog">
            <span class="value">{{ highlights.Catalog }}</span>
            <span class="label">{{
              highlights.Catalog > 1 ? "Catalogues" : "Catalogue"
            }}</span>
          </data>
        </div>
        <div class="highlight">
          <data class="data" :value="highlights.Dataset">
            <span class="value">{{ highlights.Dataset }}</span>
            <span class="label">{{
              highlights.Dataset > 1 ? "Datasets" : "Dataset"
            }}</span>
          </data>
        </div>
        <div class="highlight">
          <data class="data" :value="highlights.Distribution">
            <span class="value">{{ highlights.Distribution }}</span>
            <span class="label">{{
              highlights.Distribution > 1 ? "Distributions" : "Distribution"
            }}</span>
          </data>
        </div>
      </div>
    </PageSection>
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
        To find get started, use the dataset search for find collections or use
        the beacon search to find individual records.
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
        <LinkCard>
          <router-link :to="{ name: 'datasets' }">
            Find Datasets
            <ArrowRightCircleIcon />
          </router-link>
        </LinkCard>
        <LinkCard>
          <router-link :to="{ name: 'beacon' }">
            Search with Beacon
            <ArrowRightCircleIcon />
          </router-link>
        </LinkCard>
      </div>
    </PageSection>
    <PageSection :verticalPadding="2" aria-labelledby="about-title">
      <h2 id="about-title">About the GDI Local Portal</h2>
      <p>
        The GDI local portal implementation runs on
        <a href="https://www.molgenis.org">MOLGENIS</a> (Java, PostgreSQL,
        GraphQL) and is connected to
        <a href="https://github.com/CSCfi/rems">REMS</a>. The portal also
        includes OIDC configuration to allow users to seamlessly sign in to
        these systems. In additional, the local portal exposes the metadata
        though the
        <a href="https://www.fairdatapoint.org">Fair Data Points</a> and
        <a href="https://beacon-project.io">Beacon</a>.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onBeforeMount } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { Page, PageHeader, PageSection, LinkCard } from "molgenis-viz";
import { ArrowRightCircleIcon } from "@heroicons/vue/24/outline";

let showHighlights = ref(false);
let highlights = ref({});

async function getHighlights() {
  const query = gql`
    {
      Catalog {
        id
      }
      Dataset {
        id
      }
      Distribution {
        name
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = Object.keys(response).map((key) => [key, response[key].length]);
  highlights.value = Object.fromEntries(data);
}

onBeforeMount(() => {
  getHighlights()
    .then(() => (showHighlights.value = true))
    .catch((err) => {
      console.error(err);
      throw new Error("Unable to retrieve highlights");
    });
});
</script>
