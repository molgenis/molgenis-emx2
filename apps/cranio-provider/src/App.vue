<template>
  <Molgenis id="__top" v-model="session">
    <Page>
      <LoadingScreen v-if="loading && !error" />
      <div class="message-box-container" v-else-if="!loading && error">
        <MessageBox type="error">
          <p><{{ error }}</p>
        </MessageBox>
      </div>
      <div v-else>
        <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="provider.name"
          :imageSrc="
            Object.hasOwn(provider.image, 'id')
              ? `${provider.image.url}`
              : 'img/banner-diagnoses.jpg'
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
    <AppFooter :publicSchema="cranioPublicSchema" />
  </Molgenis>
</template>

<script setup lang="ts">
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

let loading = ref<boolean>(true);
let error = ref<Error | null>(null);
let cranioPublicSchema = ref<string | null>(null);
let schema = ref<object | null>(null);
let provider = ref<object | null>(null);

async function getCranioPublicSchema() {
  const query = gql`
    {
      _settings {
        key
        value
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const result = response._settings?.filter(
    (row) => row.key === "CRANIO_PUBLIC_SCHEMA"
  )[0];

  if (!result.value) {
    throw new Error(
      "Missing the name of the schema that controls the vue application cranio_public. In the current schema, navigate to the settings table. Add a new setting with the key 'CRANIO_PUBLIC_SCHEMA' and enter the name in the value column. Hit save and refresh the page."
    );
  } else {
    cranioPublicSchema.value = result.value;
  }
}

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

  const result = await request(
    `/${cranioPublicSchema.value}/api/graphql`,
    query
  );
  const data = result.Organisations[0];
  data.id = data.providerInformation[0].providerIdentifier;
  delete data.providerInformation;
  provider.value = data;
}

async function loadData() {
  await getSchemaMeta();
  await getProviderMeta();
}

onBeforeMount(async () => {
  await getCranioPublicSchema().catch((err) => (error.value = err));
  await loadData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>
