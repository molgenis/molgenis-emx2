<script lang="ts" setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import Container from "../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BaseIcon from "../../../../../tailwind-components/app/components/BaseIcon.vue";
import Message from "../../../../../tailwind-components/app/components/Message.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import ButtonDropdown from "../../../../../tailwind-components/app/components/button/Dropdown.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import InputSearch from "../../../../../tailwind-components/app/components/input/Search.vue";

import type { Crumb } from "../../../../../tailwind-components/types/types";
import type { IContainers } from "../../../../../tailwind-components/types/cms";
import type {
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types.js";

import { getContainersAndMetadata } from "../../../../../tailwind-components/app/gql/cmsPages.js";

interface ICmsPage extends IContainers {
  mg_tableclass: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

useHead({ title: `Pages - ${schema} - Molgenis` });

interface PagesResponse {
  data: { Containers: ICmsPage[]; _schema?: ISchemaMetaData };
  error: Record<string, any>[];
}

const error = ref<string>("");
const { data } = await $fetch<PagesResponse>(`/${schema}/graphql`, {
  method: "POST",
  body: {
    query: getContainersAndMetadata,
  },
}).catch((err) => {
  error.value = err;
  throw new Error(err);
});

const configurablePageTableMetadata = ref<ITableMetaData>();
const developerPageTableMetadata = ref<ITableMetaData>();
const formMetadata = ref();
const showFormModal = ref<boolean>(false);
const search = ref<string>();

developerPageTableMetadata.value = data._schema?.tables.filter(
  (row: ITableMetaData) => {
    return row.id === "DeveloperPages";
  }
)[0];

configurablePageTableMetadata.value = data._schema?.tables.filter(
  (row: ITableMetaData) => {
    return row.id === "ConfigurablePages";
  }
)[0];

function onAddNewPageClick(type: string) {
  if (type === "ConfigurablePage") {
    formMetadata.value = configurablePageTableMetadata.value;
  } else {
    formMetadata.value = developerPageTableMetadata.value;
  }
  showFormModal.value = true;
}

const crumbs: Crumb[] = [
  { label: schema as string, url: `/${schema}` },
  { label: "Pages", url: "" },
];

function setNuxtLink(value: string, page: string): string | undefined {
  if (value.endsWith(".Developer pages")) {
    return `/${schema}/pages/${page}/editor`;
  } else {
    return `/${schema}/pages/${page}/configure`;
  }
}
</script>

<template>
  <Container>
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <div class="flex pb-7.5 justify-between">
      <div class="w-3/5 xl:w-2/5 2xl:w-1/5">
        <label for="page-search" class="sr-only"> Search for pages </label>
        <InputSearch
          id="page-search"
          size="medium"
          v-model="search"
          placeholder="Search for pages"
        />
      </div>
      <div class="flex gap-2.5">
        <ButtonDropdown label="Add new page">
          <Button
            type="secondary"
            class="w-full"
            @click="onAddNewPageClick('ConfigurablePage')"
          >
            Simple page
          </Button>
          <Button type="secondary" @click="onAddNewPageClick('DeveloperPage')">
            developer page
          </Button>
        </ButtonDropdown>
      </div>
    </div>
    <div
      class="flex flew-wrap justify-start items-center gap-7.5"
      v-if="data && data.Containers"
    >
      <div
        v-for="container in data.Containers"
        class="relative group border rounded-3px w-1/3 h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center bg-form-legend"
      >
        <div
          class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
          v-tooltip.bottom="`Edit`"
        >
          <NuxtLink
            :to="setNuxtLink(container.mg_tableclass, container.name)"
            class="font-display tracking-widest uppercase text-heading-lg hover:underline cursor-pointer"
          >
            <BaseIcon name="Edit" :width="18" />
            <span class="sr-only">edit page</span>
          </NuxtLink>
        </div>
        <NuxtLink
          :to="`/${schema}/pages/${container.name}/`"
          class="text-button-text hover:underline"
        >
          {{ container.name }}
        </NuxtLink>
      </div>
    </div>
    <div v-else>
      <Message id="page-index-error" :invalid="true">
        <span>{{ error }}</span>
      </Message>
    </div>
  </Container>
  <EditModal
    v-if="formMetadata"
    key="edit-modal-configurable-page"
    :show-button="false"
    :schema-id="(schema as string)"
    :metadata="formMetadata"
    :is-insert="false"
    v-model:visible="showFormModal"
  />
</template>
