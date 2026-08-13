<script setup lang="ts">
import { ref } from "vue";
import { getCranioSchemaNames } from "./utils/getCranioSchemaNames";
import { getSchemaName } from "./utils/getSchemaName";
import { getOrganisation } from "./utils/getOrganisation";

import type { IMgErrorResponse, ICranioSchemas } from "./types";
import type { IOrganisations } from "../../metadata-utils/src/viz/ErnDashboard";

// @ts-ignore
import { Molgenis } from "molgenis-components";
import {
  Page,
  PageHeader,
  Dashboard,
  MessageBox,
  LoadingScreen,
  AppFooter,
  // @ts-ignore
} from "molgenis-viz";

// @ts-ignore
import ProviderSidebar from "./components/ProviderSidebar.vue";

const session = ref(null);
const loading = ref<boolean>(true);
const error = ref<string>();
const cranioSchemas = ref<ICranioSchemas>();
const currentSchemaName = ref<string>();
const currentOrganisation = ref<IOrganisations>();
const publicSchema = ref<string>();

async function getAppData() {
  cranioSchemas.value = await getCranioSchemaNames();
  currentSchemaName.value = await getSchemaName();
  currentOrganisation.value = await getOrganisation(
    `/${cranioSchemas.value?.CRANIO_PUBLIC_SCHEMA}/api/graphql`,
    currentSchemaName.value
  );
}

getAppData()
  .then(() => {
    window.document.title = `${window.document.title} | ${currentOrganisation.value?.name}`;
    publicSchema.value = cranioSchemas.value?.CRANIO_PUBLIC_SCHEMA;
  })
  .catch((err) => {
    if (Object.hasOwn(err as Error, "response")) {
      const message = (err as IMgErrorResponse).response.errors[0].message;
      error.value = message;
    } else {
      error.value = (err as Error).message;
    }
  })
  .finally(() => (loading.value = false));
</script>

<template>
  <Molgenis id="__top" v-model="session">
    <Page>
      <LoadingScreen v-if="loading && !error" />
      <div class="message-box-container" v-else-if="!loading && error">
        <MessageBox type="error">
          <p>{{ error }}</p>
        </MessageBox>
      </div>
      <div v-else>
        <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="currentOrganisation?.name"
          :imageSrc="
            currentOrganisation?.image
              ? `${currentOrganisation?.image.url}`
              : 'img/banner-diagnoses.jpg'
          "
        />
        <Dashboard class="provider-dashboard-container" :horizontalPadding="2">
          <ProviderSidebar />
          <router-view
            :organisation="currentOrganisation"
            :schemaNames="cranioSchemas"
            :api="{
              graphql: {
                current: `/${currentOrganisation?.schemaName}/api/graphql`,
                public: `/${cranioSchemas?.CRANIO_PUBLIC_SCHEMA}/api/graphql`,
                providers: `/${cranioSchemas?.CRANIO_PROVIDER_SCHEMA}/api/graphql`,
              },
            }"
          ></router-view>
        </Dashboard>
      </div>
    </Page>
    <AppFooter
      id="cranio-provider-footer"
      firstColumnTitle="ERN CRANIO"
      secondColumnTitle="For Members"
    >
      <template v-slot:column-links-1>
        <li>
          <a :href="`/${publicSchema}/cranio-public/#/`">Home</a>
        </li>
        <li>
          <a :href="`/${publicSchema}/cranio-public/#/about`">About</a>
        </li>
        <li>
          <a :href="`/${publicSchema}/cranio-public/#/dashboard`">
            Dashboard
          </a>
        </li>
      </template>
      <template v-slot:column-links-2>
        <li>
          <a :href="`/${publicSchema}/cranio-public/#/Providers`">
            Providers
          </a>
        </li>
        <li>
          <a :href="`/${publicSchema}/cranio-public/#/Documents`">
            Documents
          </a>
        </li>
      </template>
      <template v-slot:column-logos>
        <li id="project-logo-link">
          <a :href="`/${publicSchema}/cranio-public/#/`">
            <img
              src="/img/ern-cranio-logo.png"
              alt="ERN CRANIO: European Reference Network for rare and/or complex craniofacial anomalies and ear, nose and throat (ENT) disorders"
            />
          </a>
        </li>
      </template>
    </AppFooter>
  </Molgenis>
</template>
