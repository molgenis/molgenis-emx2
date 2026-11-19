<script lang="ts" setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import DeveloperPage from "../../../../../../tailwind-components/app/components/editor/HtmlPreview.vue";
import ConfigurablePage from "../../../../../../tailwind-components/app/components/cms/ConfigurablePage.vue";
import { getPage } from "../../../../../../tailwind-components/app/utils/cms";

import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type { ITableMetaData } from "../../../../../../metadata-utils/src";
import type {
  IConfigurablePages,
  IDeveloperPages,
} from "../../../../../../tailwind-components/types/cms";

const route = useRoute();
const schema = computed<string>(() => {
  const currentSchema = Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema ?? "";
  return currentSchema as string;
});

const page = computed<string>(() => route.params.page as string);

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });

const pageData = await getPage(schema.value, page.value);
const pageContent = pageData.page;

const crumbs: Crumb[] = [
  { label: schema.value, url: `/${schema}` },
  { label: "Pages", url: `/${schema}/pages` },
  { label: page.value, url: "" },
];
</script>

<template>
  <Container>
    <bread-crumbs align="left" :crumbs="crumbs" class="my-5" />
  </Container>
  <DeveloperPage
    v-if="pageContent.mg_tableclass === 'cms.Developer pages'"
    :content="(pageData.page as IDeveloperPages)"
  />
  <ConfigurablePage
    v-else
    :content="(pageData.page as IConfigurablePages)"
    :metadata="(pageData.metadata as ITableMetaData[])"
    :isEditable="false"
    :schema="schema || ''"
    :route="route"
  />
</template>
