<script lang="ts" setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import DeveloperPage from "../../../../../../tailwind-components/app/components/editor/HtmlPreview.vue";
import ConfigurablePage from "../../../../../../tailwind-components/app/components/cms/ConfigurablePage.vue";
import { getPage } from "../../../../../../tailwind-components/app/utils/cms";
import { useSession } from "../../../../../../tailwind-components/app/composables/useSession.ts";

import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type { ITableMetaData } from "../../../../../../metadata-utils/src";
import type {
  IConfigurablePages,
  IDeveloperPages,
} from "../../../../../../tailwind-components/types/cms";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

const page = route.params.page as string;

const { isAdmin, session } = await useSession(schema);
const enableEditing = computed(() => {
  return (
    session.value?.roles?.[schema as string]?.includes("Manager") ||
    isAdmin.value
  );
});

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });

const pageData = await getPage(schema as string, page);
const pageContent = pageData.page;

const crumbs: Crumb[] = [
  { label: schema as string, url: `/${schema}` },
  { label: "Pages", url: `/${schema}/pages` },
  { label: page as string, url: "" },
];

const url = computed<string>(() => {
  return "";
});

const urlLabel = computed<string>(() => {
  return enableEditing.value ? "Edit page" : "Preview page";
});
</script>

<template>
  <Container>
    <bread-crumbs align="left" :crumbs="crumbs" class="my-5" />
  </Container>
  <div class="relative">
    <DeveloperPage
      v-if="pageContent.mg_tableclass === 'cms.Developer pages'"
      :content="(pageData.page as IDeveloperPages)"
    />
    <ConfigurablePage
      v-else
      :content="(pageData.page as IConfigurablePages)"
      :metadata="(pageData.metadata as ITableMetaData[])"
      :is-editable="false"
      :schema="schema || ''"
    />
    <div class="fixed bottom-10 right-10" v-if="enableEditing">
      <NuxtLink
        :to="url"
        v-tooltip.top="urlLabel"
        class="flex justify-center items-center rounded-full bg-button-primary text-button-primary p-2.5 cursor-pointer"
      >
        <BaseIcon :name="enableEditing ? 'Edit' : 'Visible'" />
        <span class="sr-only">{{ urlLabel }}</span>
      </NuxtLink>
    </div>
  </div>
</template>
