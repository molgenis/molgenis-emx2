<script lang="ts" setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import ConfigurablePage from "../../../../../../tailwind-components/app/components/pages/ConfigurablePage.vue";
import { getPage } from "../../../../../../tailwind-components/app/utils/cms";

import { useSession } from "../../../../../../tailwind-components/app/composables/useSession";

import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type { ITableMetaData } from "../../../../../../metadata-utils/src";
import type { IConfigurablePages } from "../../../../../../tailwind-components/types/cms";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;

useHead({ title: `Edit - ${page} - Pages - ${schema} - Molgenis` });

const pageData = await getPage(schema as string, page);

const crumbs: Crumb[] = [
  { label: schema as string, url: `/${schema}` },
  { label: "Pages", url: `/${schema}/pages` },
  { label: page as string, url: "" },
];

const { isAdmin, session } = await useSession(schema);
const enableEditing = computed(() => {
  return (
    session.value?.roles?.[schema as string]?.includes("Editor") ||
    isAdmin.value
  );
});
</script>

<template>
  <Container>
    <bread-crumbs align="left" :crumbs="crumbs" class="my-5" />
  </Container>
  <ConfigurablePage
    :content="(pageData.page as IConfigurablePages)"
    :metadata="(pageData.metadata as ITableMetaData[])"
    :is-editable="enableEditing"
  />
</template>
