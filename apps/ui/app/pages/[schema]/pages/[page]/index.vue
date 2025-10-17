<script lang="ts" setup>
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import HtmlPreview from "../../../../../../tailwind-components/app/components/editor/HtmlPreview.vue";
import { getPage } from "../../../../../../tailwind-components/app/utils/Pages";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });

const developerPage = await getPage(schema as string, page);

const crumbs: Record<string, string> = {};
crumbs[schema as string] = `/${schema}`;
crumbs["Pages"] = `/${schema}/pages`;
crumbs[page as string] = "";
</script>

<template>
  <Container>
    <bread-crumbs align="left" :crumbs="crumbs" class="my-5" />
  </Container>
  <HtmlPreview :content="developerPage" />
</template>
