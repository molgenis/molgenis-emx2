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
              ? `${currentOrganisation?.image?.url}`
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
            :organisation="currentOrganisation"
            :schemaNames="cranioSchemas"
          ></router-view>
        </Dashboard>
      </div>
    </Page>
    <AppFooter :publicSchema="cranioSchemas?.CRANIO_PUBLIC_SCHEMA" />
  </Molgenis>
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import { getCranioSchemaNames } from "./utils/getCranioSchemaNames";
import { getSchemaName } from "./utils/getSchemaName";
import { getOrganisation } from "./utils/getOrganisation";

import type { IMgErrorResponse, ICranioSchemas } from "./interfaces";
import type { IOrganisations } from "./interfaces/schema";
import type { IAppPage } from "./interfaces/app";

// @ts-ignore
import { Molgenis } from "molgenis-components";
import {
  Page,
  PageHeader,
  PageSection,
  Dashboard,
  MessageBox,
  LoadingScreen,
  // @ts-ignore
} from "molgenis-viz";

// @ts-ignore
import ProviderSidebar from "./components/ProviderSidebar.vue";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);

const loading = ref<boolean>(true);
const error = ref<string>();
const cranioSchemas = ref<ICranioSchemas>();
const currentSchemaName = ref<string>();
const currentOrganisation = ref<IOrganisations>();

onBeforeMount(async () => {
  try {
    cranioSchemas.value = await getCranioSchemaNames();
    currentSchemaName.value = await getSchemaName();
    currentOrganisation.value = await getOrganisation(
      `/${cranioSchemas.value?.CRANIO_PUBLIC_SCHEMA}/api/graphql`,
      currentSchemaName.value
    );
  } catch (err: unknown) {
    if (Object.hasOwn(err as Error, "response")) {
      const message = (err as IMgErrorResponse).response.errors[0].message;
      error.value = message;
    } else {
      error.value = (err as Error).message;
    }
  } finally {
    loading.value = false;
  }
});
</script>
