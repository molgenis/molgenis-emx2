<script lang="ts" setup>
import { useRoute } from "vue-router";
import { useHead, useFetch } from "#app";

import Container from "../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BaseIcon from "../../../../../tailwind-components/app/components/BaseIcon.vue";
import Message from "../../../../../tailwind-components/app/components/Message.vue";
import type { Pages } from "../../../../../tailwind-components/app/utils/Pages";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;

useHead({ title: `Pages - ${schema} - Molgenis` });

interface PagesResponse {
  data: { Pages: Pages[] };
}

const { data } = await useFetch<PagesResponse>(`/${schema}/graphql`, {
  key: "tables",
  method: "POST",
  body: {
    query: `{ Pages { name mg_tableclass } }`,
  },
});

const crumbs: Record<string, string> = {};
crumbs[schema as string] = `/${schema}`;
crumbs["Pages"] = "";
</script>

<template>
  <Container>
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <div
      class="flex flew-wrap justify-start items-center gap-7.5"
      v-if="data?.data.Pages"
    >
      <div
        v-for="customPage in data.data.Pages"
        class="relative group border rounded-3px w-56 h-36 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center"
      >
        <div
          class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
        >
          <NuxtLink
            :to="`/${schema}/pages/${customPage.name}/edit`"
            class="font-display tracking-widest uppercase text-heading-lg hover:underline cursor-pointer"
            v-tooltip.bottom="`Edit`"
          >
            <BaseIcon name="Edit" :width="18" />
            <span class="sr-only">edit page</span>
          </NuxtLink>
        </div>
        <NuxtLink
          :to="`/${schema}/pages/${customPage.name}/`"
          class="text-button-text hover:underline"
        >
          {{ customPage.name }}
        </NuxtLink>
      </div>
    </div>
    <div v-else>
      <Message id="page-index-message" :invalid="true">
        <span>No pages found.</span>
      </Message>
    </div>
  </Container>
</template>
