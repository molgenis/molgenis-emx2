<script lang="ts" setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { useAsyncData } from "nuxt/app";

import Container from "../../../../../tailwind-components/app/components/Container.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BaseIcon from "../../../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import ButtonDropdown from "../../../../../tailwind-components/app/components/button/Dropdown.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";

import type { Crumb } from "../../../../../tailwind-components/types/types";

import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchTableData from "../../../../../tailwind-components/app/composables/fetchTableData";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? (route.params.schema[0] as string)
  : route.params.schema ?? "";

useHead({ title: `Pages - ${schema} - Molgenis` });

const crumbs: Crumb[] = [
  { label: schema as string, url: `/${schema}` },
  { label: "Pages", url: "" },
];

const formMetadata = ref();
const showFormModal = ref<boolean>(false);

const { data, refresh } = useAsyncData(`containers-${schema}`, async () => {
  const configurablePageMetadata = await fetchTableMetadata(
    schema,
    "ConfigurablePages"
  );
  const developerPageMetadata = await fetchTableMetadata(
    schema,
    "DeveloperPages"
  );
  const containers = await fetchTableData(schema, "Containers", {
    orderby: { name: "ASC" },
  });

  return {
    configurablePageMetadata,
    developerPageMetadata,
    containers,
  };
});

function onAddNewPageClick(type: string) {
  if (type === "ConfigurablePage") {
    formMetadata.value = data.value?.configurablePageMetadata;
  } else {
    formMetadata.value = data.value?.developerPageMetadata;
  }
  showFormModal.value = true;
}

async function onClose() {
  await refresh();
}

function setNuxtLink(value: string, page: string): string {
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
      <div class="w-3/5 xl:w-2/5 2xl:w-1/5" />
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
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 flew-wrap justify-start items-center gap-7.5"
      v-if="data && data.containers"
    >
      <div
        v-for="container in data.containers.rows"
        class="relative group border rounded-3px w-full h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center bg-form-legend"
      >
        <div
          class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
          v-tooltip.bottom="`Edit`"
        >
          <NuxtLink
            :to="setNuxtLink((container.mg_tableclass as string), (container.name as string))"
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
  </Container>
  <EditModal
    v-if="formMetadata"
    key="edit-modal-configurable-page"
    :showButton="false"
    :schemaId="(schema as string)"
    :metadata="formMetadata"
    :isInsert="true"
    v-model:visible="showFormModal"
    @update:cancelled="onClose"
  />
</template>
