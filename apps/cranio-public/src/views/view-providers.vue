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
      <p>Sunt do dolore non ex cillum mollit officia est laboris ex exercitation qui cillum. Elit culpa non voluptate occaecat ut sit qui voluptate quis sint incididunt dolore Lorem enim. Labore ad dolore est sit. Cupidatat nulla et commodo amet amet excepteur ut ex sint amet voluptate.</p>
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
            <p class="city">{{ provider.city }}</p>
            <p class="country">{{ provider.country }}</p>
          </div>
          <div class="link">
            <a :href="`/${provider.id}`">
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
import { Page, PageHeader, PageSection, MessageBox, sortData } from "molgenis-viz";
import { postQuery } from "../utils/utils";

import { ChevronRightIcon as LinkIcon } from "@heroicons/vue/24/outline"

let error = ref(false);
let providers = ref([]);

async function getOrganisations () {
  const response = await postQuery(
    "/api/graphql",
    `{
      Organisations {
        name
        city
        country
        latitude
        longitude
        providerInformation {
          providerIdentifier
        }
      }
    }`
  )
  
  const data = await response.data.Organisations
    .map(row => {
      return {...row, id: row.providerInformation[0].providerIdentifier}
    });
  return data
}

async function loadData() {
  providers.value = await getOrganisations();
}

onMounted(() => {
  Promise.resolve(loadData())
  .then(() => {
    providers.value = sortData(providers.value, "name");
  })
  .catch(err => error.value = err);
})
</script>

<style lang="scss">
.provider-listings {
  padding: 1em 0;
  
  $borderRadius: 24pt;

  .provider {    
    display: grid;
    grid-template-columns: repeat(2, 1fr) 100px;
    justify-content: center;
    align-items: center;
    box-sizing: content-box;
    background-color: $gray-000;
    padding: 1em;
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
      padding: 0 1.5em; 
      h3 {
        text-align: left;
        color: $blue-900; 
        font-size: 13pt;
      }
    }
    
    .location {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
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
  
  }
}
</style>