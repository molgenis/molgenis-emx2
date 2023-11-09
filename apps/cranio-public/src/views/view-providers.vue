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
    <PageSection class="bg-blue-050" :verticalPadding="2">
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
          <div class="provider-data name">
            <h3>{{ provider.name }}</h3>
          </div>
          <div class="provider-data city">
            <p>{{ provider.city }}</p>
          </div>
          <div class="provider-data country">
            <p>{{ provider.country }}</p>
          </div>
          <div class="provider-data link">
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
import gql from "graphql-tag";
import { request } from "graphql-request";
import Breadcrumbs from "../components/breadcrumbs.vue";
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import { ChevronRightIcon as LinkIcon } from "@heroicons/vue/24/outline";

let error = ref(false);
let providers = ref([]);

async function getOrganisations() {
  const query = gql`
    {
      Organisations(filter: { hasSchema: { equals: true } }) {
        name
        city
        country
        providerInformation {
          providerIdentifier
        }
        schemaName
      }
    }
  `;

  const response = await request("../api/graphql", query);
  providers.value = response.Organisations.map((row) => {
    return {
      id: row.providerInformation[0].providerIdentifier,
      ...row,
    };
  }).sort((current, next) => (current.name < next.name ? -1 : 1));
}

onMounted(() => {
  getOrganisations().catch((err) => (error.value = err));
});
</script>

<style lang="scss">
$borderRadius: 24pt;

.provider-listings {
  padding: 1em 0;

  .provider {
    display: grid;
    justify-content: flex-start;
    align-items: center;
    grid-template-columns: repeat(2, 1fr);
    grid-template-areas: 
      "name name"
      "city country"
      "link link";
    gap: 1em;
    background-color: $gray-000;
    border-radius: $borderRadius;
    box-sizing: content-box;
    padding: 1em 1.5em;
    margin-bottom: 1.3em;
    
    &:last-child {
      margin-bottom: 0;
    }
      
    .provider-data { 
      flex-grow: 1;
      font-size: 13pt;
      p {
        margin-bottom: 0;
      }
    }
    
    .name {
      grid-area: name;
      word-break: break-word;

      h3 {
        font-size: 13pt;
        color: $blue-800;
      }
    }
    
    .city {
      grid-area: city;
      text-align: right;
    }
    
    .country {
      grid-area: country;
    } 
      
    .link {
      grid-area: link;
      
      a {
        display: block;
        text-align: center;
        border-radius: $borderRadius;
        background-color: $ern-cranio-primary;
        padding: 0.6em 0.2em;
        color: $gray-000;
        @include textTransform(bold);
        font-size: 0.85rem;
        text-decoration: none;
        
        svg {
          margin-top: -2px;
          width: 12px;
          height: 12px;
          path {
            stroke-width: 5px;
          }
        }
      }
    }
    
    @media (min-width: 636px) {
      grid-template-columns: 2fr 1fr 1fr 100px;
      grid-template-areas: "name city country link";
      gap: 0.2em;
      .name {
        h3 {
          text-align: left;
        }
      }
      .city {
        text-align: center;
      }
      
      .country {
        text-align: center;
      }
    }
  }
}
</style>
