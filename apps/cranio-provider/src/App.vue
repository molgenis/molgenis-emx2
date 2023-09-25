<template>
  <Molgenis id="__top" v-model="session"> 
    <Page>
      <LoadingScreen v-if="loading && !error"/>
      <div v-else-if="!loading && error">
        <MessageBox type="error">
          <p>Unable to retrieve results. {{ error }}</p>
        </MessageBox>
      </div>
      <div v-else>
        <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="schema"
          imageSrc="profiles/erasmus-mc.jpg"
        />
        <Dashboard class="provider-dashboard-container">
          <ProviderSidebar />
          <router-view
            providerId="schema"
            providerName="Test"
            providerImageUrl="profiles/erasmus-mc.jpg"
          >
          </router-view>
        </Dashboard>
      </div>
    </Page>
  </Molgenis>
</template>

<script setup>
import { ref, onBeforeMount } from "vue";
import { Molgenis } from "molgenis-components";
import { Page, PageHeader, Dashboard, MessageBox, LoadingScreen } from "molgenis-viz";
import ProviderSidebar from "./components/ProviderSidebar.vue";
// import AppFooter from "./components/AppFooter.vue";

import { postQuery } from "./utils/utils";

const session = ref(null);
const page = ref(null);
let loading = ref(true);
let error = ref(null);
let schema = ref(null);
let data = ref(null);

async function getSchemaMeta () {
  const result = await postQuery(
    '/api/graphql',
    '{ _schema { name }}'
  )
  const data = await result.data._schema.name
  return data
}

async function getProviderMeta (id) {
  const result = await postQuery(
    '/pet%20store/graphql',
    "{ _schema { name }}"
    // `{
    //   Organisations (
    //     filter: {
    //       providerInformation: {
    //         providerIdentifier: {
    //           equals: "${id}"
    //         }
    //       }
    //     }
    //   ) {
    //     name
    //     imageUrl
    //     providerInformation {
    //       providerIdentifier
    //     }
    //   }
    // }`
  )
  
  const data = await result.data;
  return data
}

onBeforeMount(() => { 
  getSchemaMeta()
  .then(response => schema.value = response)
  .then(() => loading.value = false)
  .catch(err => err.value = error);
  
  getProviderMeta()
  .then(response => console.log(response))
})


</script>
