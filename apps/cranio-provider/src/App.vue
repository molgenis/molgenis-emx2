<template>
  <Molgenis id="__top" v-model="session">
    <Page>
      <LoadingScreen v-if="loading && !error" />
      <div class="message-box-container" v-else-if="!loading && error">
        <MessageBox type="error">
          <p>Unable to retrieve results. {{ error }}</p>
        </MessageBox>
      </div>
      <div v-else>
        <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="provider.name"
          :imageSrc="
            provider.image ? `${provider.image.url}.${provider.image.extension}` : 'banner-diagnoses.jpg'
          "
        />
        <PageSection
          class="bg-gray-050"
          aria-labelledby="temp-message-title"
          :verticalPadding="1"
        >
          <h2 id="temp-message-title" class="visually-hidden">
            About the dashboards
          </h2>
          <MessageBox type="warning">
            <p>
              This dashboard is currently under development. All data shown in
              the following visualisations have been randomly generated for
              demonstration purposes.
            </p>
          </MessageBox>
        </PageSection>
        <Dashboard class="provider-dashboard-container" :horizontalPadding="5">
          <ProviderSidebar />
          <router-view
            :providerId="provider.id"
            :providerName="provider.name"
          ></router-view>
        </Dashboard>
      </div>
    </Page>
    <AppFooter />
  </Molgenis>
</template>

<script setup>
import { ref, onBeforeMount } from "vue";
import { Molgenis } from "molgenis-components";
import {
  Page,
  PageHeader,
  PageSection,
  Dashboard,
  MessageBox,
  LoadingScreen,
} from "molgenis-viz";
import ProviderSidebar from "./components/ProviderSidebar.vue";
import AppFooter from "./components/AppFooter.vue";

import gql from "graphql-tag";
import { request } from "graphql-request";

const session = ref(null);
const page = ref(null);

let loading = ref(true);
let error = ref(null);
let schema = ref(null);
let provider = ref(null);

async function getSchemaMeta() {
  const query = gql`
    {
      _schema {
        name
      }
    }
  `;
  const result = await request("../api/graphql", query);
  schema.value = result._schema.name;
}

async function getProviderMeta() {
  const query = gql`{
    Organisations (
      filter: {
        providerInformation: {
          providerIdentifier: {
            equals: "${schema.value}"
          }
        }
      }
    ) {
      name
      image {
        id
        url
        size
        extension
      }
      providerInformation {
        providerIdentifier
      }
    }
  }`;

  const result = await request("/CranioStats/api/graphql", query);
  const data = result.Organisations[0];
  data.id = data.providerInformation[0].providerIdentifier;
  delete data.providerInformation;
  provider.value = data;
}

async function loadData() {
  await getSchemaMeta();
  await getProviderMeta();
}

onBeforeMount(() => {
  loadData()
    .then(() => (loading.value = false))
    .catch((err) => {
      loading.value = false;
      error.value = err;
    });
});
</script>
