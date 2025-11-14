<script lang="ts" setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BaseIcon from "../../../../../tailwind-components/app/components/BaseIcon.vue";
import Message from "../../../../../tailwind-components/app/components/Message.vue";
import type { Pages } from "../../../../../tailwind-components/app/utils/Pages";
import type { Crumb } from "../../../../../tailwind-components/types/types";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

useHead({ title: `Pages - ${schema} - Molgenis` });

interface PagesResponse {
  data: { Page: Pages[] };
  error: Record<string, any>[];
}

const error = ref<string>("");
const { data } = await $fetch<PagesResponse>(`/${schema}/graphql`, {
  method: "POST",
  body: {
    query: `{ Page { name mg_tableclass } }`,
  },
}).catch((err) => {
  error.value = err;
  throw new Error(err);
});

const crumbs: Crumb[] = [
  { label: schema as string, url: `/${schema}` },
  { label: "Pages", url: "" },
];
</script>

<template>
  <Container>
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <div class="flex flew-wrap justify-start items-center gap-7.5" v-if="data">
      <div
        v-for="customPage in data.Page"
        class="relative group border rounded-3px w-1/3 h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center bg-form-legend"
      >
        <div
          class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
          v-tooltip.bottom="`Edit`"
        >
          <NuxtLink
            :to="`/${schema}/pages/${customPage.name}/edit`"
            class="font-display tracking-widest uppercase text-heading-lg hover:underline cursor-pointer"
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
      <Message id="page-index-error" :invalid="true">
        <span>{{ error }}</span>
      </Message>
    </div>
  </Container>
</template>
