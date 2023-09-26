<template>
  <Molgenis id="__top" v-model="session">
    <Page>
      <LoadingScreen v-if="loading && !error" />
      <div v-else-if="!loading && error">
        <MessageBox type="error">
          <p>Unable to retrieve results. {{ error }}</p>
        </MessageBox>
      </div>
      <div v-else>
        <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="provider.name"
          :imageSrc="provider.imageUrl"
        />
        <Dashboard class="provider-dashboard-container">
          <ProviderSidebar />
          <router-view
            :providerId="provider.id"
            :providerName="provider.name"
          ></router-view>
        </Dashboard>
      </div>
    </Page>
  </Molgenis>
</template>

<script setup>
import { ref, onBeforeMount } from "vue";
import { Molgenis } from "molgenis-components";
import {
  Page,
  PageHeader,
  Dashboard,
  MessageBox,
  LoadingScreen,
} from "molgenis-viz";
import ProviderSidebar from "./components/ProviderSidebar.vue";

import { postQuery } from "./utils/utils";

const session = ref(null);
const page = ref(null);

let loading = ref(true);
let error = ref(null);
let schema = ref(null);
let provider = ref(null);

async function getSchemaMeta() {
  const result = await postQuery("./api/graphql", "{ _schema { name }}");
  const data = await result.data._schema.name;
  return data;
}

async function getProviderMeta(id) {
  const result = await postQuery(
    "/CranioStats/api/graphql",
    `{
      Organisations (
        filter: {
          providerInformation: {
            providerIdentifier: {
              equals: "${id}"
            }
          }
        }
      ) {
        name
        imageUrl
        providerInformation {
          providerIdentifier
        }
      }
    }`
  );

  const data = await result.data.Organisations[0];
  return data;
}

async function loadData() {
  schema.value = await getSchemaMeta();
  provider.value = await getProviderMeta(schema.value);
  provider.value.id = provider.value.providerInformation[0].providerIdentifier;
  delete provider.value.providerInformation;
}

onBeforeMount(() => {
  loadData()
    .then(() => (loading.value = false))
    .catch((err) => (error.value = err));
});
</script>
