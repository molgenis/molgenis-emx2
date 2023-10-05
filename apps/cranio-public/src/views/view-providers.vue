<template>
  <Page>
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="ERN for rare complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
      imageSrc="banner-diagnoses.jpg"
    />
    <Breadcrumbs />
    <PageSection aria-labelledby="providers-list-title">
      <h2 id="providers-list-title">Providers</h2>
      <p>
        In the ERN Cranio registry, each Healthcare provider has their dashboard
        which provide an overview of the patients submitted to the registry. In
        the list below, you can view the dashboards that you have access to.
      </p>
    </PageSection>
    <PageSection class="section-bg-light-blue" :verticalPadding="2">
      <h2>View Providers</h2>
      <MessageBox type="error" v-if="error">
        <p>Unable to retrieve data {{ error }}</p>
      </MessageBox>
      <div class="provider-listings" v-else>
        <div
          v-for="provider in providers"
          class="provider"
          :data-provider-id="provider.id"
        >
          <div class="name">
            <h3>{{ provider.name }}</h3>
          </div>
          <div class="location">
            <p>{{ provider.city }}</p>
            <p>{{ provider.country }}</p>
          </div>
          <div class="link">
            <a :href="`/${provider.schemaName}/`">
              <span>View</span>
              <LinkIcon />
            </a>
          </div>
        </div>
      </div>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import Breadcrumbs from "../components/breadcrumbs.vue";
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import gql from "graphql-tag";
import { request } from "graphql-request";

import { ChevronRightIcon as LinkIcon } from "@heroicons/vue/24/outline";

let error = ref(false);
let providers = ref([]);

async function getOrganisations() {
  const query = gql`{
    Organisations (
      filter: {
        hasSchema: { equals: true }
      }
    ) {
      name
      city
      country
      latitude
      longitude
      providerInformation {
        providerIdentifier
        hasSubmittedData
      }
      hasSchema
      schemaName
    }
  }`;

  const response = await request("../api/graphql", query);
  providers.value = response.Organisations
    .sort((current, next) => current.name < next.name ? -1 : 1);
}

async function loadData() {
  await getOrganisations();
}

onMounted(() => {
  loadData().catch((err) => (error.value = err));
});
</script>

<style lang="scss">
.provider-listings {
  padding: 1em 0;

  $borderRadius: 24pt;

  .provider {
    display: grid;
    grid-template-columns: 1fr;
    justify-content: center;
    align-items: center;
    box-sizing: content-box;
    background-color: $gray-000;
    padding: 1em 1.5em;
    margin-bottom: 1.3em;
    border-radius: $borderRadius;

    &:last-child {
      margin-bottom: 0;
    }

    div {
      flex-grow: 1;
      font-size: 13pt;
    }

    .name { 
      h3 {
        text-align: center;
        color: $blue-900;
        font-size: 13pt;
      }
    }

    .location {
      display: flex;
      justify-content: center;
      gap: 0.5em;
      p { 
        margin: 0;
        color: $blue-700; 
      }
    }

    .link {
      a {
        display: block;
        text-align: center;
        padding: 8px 0;
        border-radius: $borderRadius;
        background-color: $ern-cranio-primary;
        color: $gray-000;
        @include textTransform(bold);
        font-size: 10pt;

        svg {
          width: 11pt;
          stroke-width: 3px;
          margin-top: -4px;
        }
      }
    }
    
    @media screen and (min-width: 636px) {
      grid-template-columns: repeat(2, 1fr) 100px;
      
      .name {
        h3 {
          text-align: left;
        }
      }
      
      .location {
        display: grid;
        grid-template-columns: repeat(2,1fr);
      }
    }
  }
}
</style>
