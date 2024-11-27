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
        <!-- <PageHeader
          title="ERN CRANIO Registry"
          :subtitle="currentOrganisation?.name"
          :imageSrc="
            currentOrganisation?.image
              ? `${currentOrganisation?.image?.url}`
              : 'img/banner-diagnoses.jpg'
          "
        /> -->
        <Dashboard class="provider-dashboard-container" :horizontalPadding="5">
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
    <AppFooter :publicSchema="cranioSchemas?.CRANIO_PUBLIC_SCHEMA" />
  </Molgenis>
</template>

<script setup lang="ts">
import { ref, onBeforeMount } from "vue";
import { getCranioSchemaNames } from "./utils/getCranioSchemaNames";
import { getSchemaName } from "./utils/getSchemaName";
import { getOrganisation } from "./utils/getOrganisation";

import type { IMgErrorResponse, ICranioSchemas } from "./types";
import type { IOrganisations } from "./types/schema";

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
