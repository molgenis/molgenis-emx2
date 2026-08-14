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
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import Message from "../../../../../tailwind-components/app/components/Message.vue";
import NoResultsMessage from "../../../../../tailwind-components/app/components/text/NoResultsMessage.vue";

import GalleryList from "../../../../../tailwind-components/app/components/cms/gallery/GalleryList.vue";

import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchTableData from "../../../../../tailwind-components/app/composables/fetchTableData";

import type { IContainers } from "../../../../../tailwind-components/types/cms.js";
import type { Crumb } from "../../../../../tailwind-components/types/types";

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
const showPageDropdown = ref<boolean>(false);

const { data, refresh, error } = useAsyncData(
  `containers-${schema}`,
  async () => {
    const configurablePageMetadata = await fetchTableMetadata(
      schema,
      "ConfigurablePages"
    );
    const developerPageMetadata = await fetchTableMetadata(
      schema,
      "DeveloperPages"
    );

    const configurablePages = await fetchTableData(
      schema,
      "ConfigurablePages",
      {
        orderby: { name: "ASC" },
      }
    );

    const developerPages = await fetchTableData(schema, "DeveloperPages", {
      orderby: { name: "ASC" },
    });

    return {
      configurablePages,
      configurablePageMetadata,
      developerPages,
      developerPageMetadata,
    };
  }
);

function onAddNewPageClick(type: string) {
  showPageDropdown.value = false;
  if (type === "ConfigurablePage") {
    formMetadata.value = data.value?.configurablePageMetadata;
  } else {
    formMetadata.value = data.value?.developerPageMetadata;
  }
  showFormModal.value = true;
}

async function onClose() {
  await refresh();
  formMetadata.value = undefined;
}

function scrollIntoView(event: Event) {
  const targetElem = (event.target as HTMLElement).getAttribute("href");
  if (targetElem) {
    console.log("scrolling toString", targetElem);
    const id: string = (targetElem as string).replace("#", "");
    document.getElementById(`${id}`)?.scrollIntoView();
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
    <div
      class="flex flex-wrap-reverse gap-7.5 md:gap-0 md:no-wrap md:justify-between pb-7.5"
    >
      <div
        class="flex items-end w-3/5"
        :class="{
          'border-b':
            data?.configurablePages.rows || data?.developerPages?.rows,
        }"
      >
        <aside
          v-if="data?.configurablePages.rows || data?.developerPages?.rows"
        >
          <h2 id="on-this-page-heading" class="sr-only">On this page</h2>
          <nav aria-labelledby="on-this-page-heading">
            <ol
              class="flex justify-center items-center gap-7.5 text-button-text [&_li]:pb-2.5 [&_li]:px-7.5 [&_li]:border-b-2 [&_li]:border-b-transparent"
            >
              <li
                class="hover:border-b-current"
                v-if="data?.configurablePages.rows"
              >
                <a href="#configurable-pages" @click.prevent="scrollIntoView">
                  Configurable pages
                </a>
              </li>
              <li
                class="hover:border-b-current"
                v-if="data?.developerPages?.rows"
              >
                <a href="#developer-pages" @click.prevent="scrollIntoView">
                  Developer pages
                </a>
              </li>
            </ol>
          </nav>
        </aside>
      </div>
      <div class="flex gap-2.5">
        <div class="relative">
          <Button
            id="openAddNewPageDropdown"
            type="outline"
            icon="CaretDown"
            iconPosition="right"
            :aria-expanded="showPageDropdown"
            aria-controls="addNewPageDropdown"
            @click="showPageDropdown = !showPageDropdown"
          >
            Add new page
          </Button>
          <div
            id="addNewPageDropdown"
            aria-labelledby="openAddNewPageDropdown"
            class="absolute z-10 w-full shadow-md rounded-base"
            :class="{
              block: showPageDropdown,
              hidden: !showPageDropdown,
            }"
          >
            <Button
              id="addNewConfigurablePageBtn"
              type="secondary"
              class="w-full"
              @click="onAddNewPageClick('ConfigurablePage')"
            >
              Simple page
            </Button>
            <Button
              id="addNewDeveloperPageBtn"
              type="secondary"
              class="w-full"
              @click="onAddNewPageClick('DeveloperPage')"
            >
              developer page
            </Button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="data?.configurablePages?.rows" class="mb-7.5">
      <GalleryList
        pageType="Configurable pages"
        :containers="(data.configurablePages.rows as unknown as IContainers[])"
        :schema="schema"
      />
    </div>
    <div v-if="data?.developerPages?.rows">
      <GalleryList
        pageType="Developer pages"
        :containers="(data.developerPages.rows as unknown as IContainers[])"
        :schema="schema"
      />
    </div>
    <div
      v-else-if="!data?.configurablePages?.rows && !data?.developerPages?.rows"
      class="w-full text-center"
    >
      <NoResultsMessage
        label="No pages found. Add a new page to get started."
      />
    </div>
    <div v-else-if="error">
      <Message id="pages-schema-error" :invalid="true">
        {{ error }}
      </Message>
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
