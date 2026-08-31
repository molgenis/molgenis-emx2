<script lang="ts" setup>
import { ref, computed } from "vue";
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
import PageSelector from "../../../../../tailwind-components/app/components/cms/gallery/PageSelector.vue";

import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchTableData from "../../../../../tailwind-components/app/composables/fetchTableData";
import {
  setCmsEditorUrl,
  setCmsViewUrl,
  newDeveloperPage,
  addBlock,
  addComponent,
  randomId,
} from "../../../../../tailwind-components/app/utils/cms";

import { useSession } from "../../../../../tailwind-components/app/composables/useSession";

import type { Crumb } from "../../../../../tailwind-components/types/types";
import type { IContainers } from "../../../../../tailwind-components/types/cms";
import type { ICmsPageTypes } from "../../../../../tailwind-components/types/CmsComponents.js";

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
const formValues = ref();
const pageType = defineModel<ICmsPageTypes | undefined>();
const showFormModal = ref<boolean>(false);
const visible = defineModel<boolean>("visible");

const { isAdmin, session } = await useSession(schema);
const enableEditing = computed(() => {
  return (
    session.value?.roles?.[schema as string]?.includes("Manager") ||
    isAdmin.value
  );
});

function onCancel() {
  visible.value = false;
}

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
    const containers = await fetchTableData(schema, "Containers", {
      orderby: { name: "ASC" },
    });

    return {
      configurablePageMetadata: configurablePageMetadata,
      developerPageMetadata: developerPageMetadata,
      containers: containers.rows as unknown as IContainers[],
    };
  }
);

function onCreatePage() {
  visible.value = false;
  formValues.value = null;
  showFormModal.value = false;
  if (pageType.value === "ConfigurablePage") {
    formMetadata.value = data.value?.configurablePageMetadata;
  } else if (pageType.value === "DeveloperPage") {
    formMetadata.value = data.value?.developerPageMetadata;
    const newPage = newDeveloperPage();
    formValues.value = newPage;
  } else {
    return undefined;
  }
  showFormModal.value = true;
}

async function onClose() {
  showFormModal.value = false;
  formMetadata.value = undefined;
  formValues.value = undefined;
  pageType.value = undefined;
  await refresh();
}

async function onAddFormValues(value: IContainers) {
  if (pageType.value === "ConfigurablePage" && value.name) {
    const bannerId = `Header-${randomId()}`;
    const sectionId = `Section-${randomId()}`;
    const headingId = `Heading-${randomId()}`;
    const paragraphId = `Paragraph-${randomId()}`;

    try {
      await addBlock(schema, bannerId, value.name, 0, "Header");
      await addBlock(schema, sectionId, value.name, 1, "Section");
      await addComponent(schema, headingId, sectionId, 0, "Heading");
      await addComponent(schema, paragraphId, sectionId, 1, "Paragraph");

      await onClose();
    } catch (error) {
      const message: string = `Unable to save page:\n${error}`;
      console.error(message);
      throw new Error(message);
    }
  } else {
    await onClose();
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
        <Button
          v-if="enableEditing"
          id="openAddNewPageDropdown"
          type="outline"
          :aria-expanded="visible"
          aria-controls="addNewPageDropdown"
          @click="visible = true"
        >
          Add new page
        </Button>
      </div>
    </div>
    <div
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 flew-wrap justify-start items-center gap-7.5"
      v-if="data?.containers"
    >
      <div
        v-for="container in data.containers"
        class="relative group border rounded-base w-full h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center bg-form-legend"
      >
        <div
          v-if="enableEditing"
          class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
          v-tooltip.bottom="`Edit`"
        >
          <NuxtLink
            :to="setCmsEditorUrl(schema, (container.mg_tableclass as string), container.name)"
            class="font-display tracking-widest uppercase text-heading-lg hover:underline cursor-pointer"
          >
            <BaseIcon name="Edit" :width="18" />
            <span class="sr-only">edit page</span>
          </NuxtLink>
        </div>
        <NuxtLink
          :to="setCmsViewUrl(schema, container.name)"
          class="text-button-text hover:underline"
        >
          {{ container.name }}
        </NuxtLink>
      </div>
    </div>
    <div v-else-if="!data?.containers" class="w-full text-center">
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
  <Modal
    v-model:visible="visible"
    max-width="max-w-9/10"
    @closed="onClose"
    title="Page selector"
  >
    <div class="min-h-0 p-12.5">
      <form @submit.prevent>
        <div class="mb-5 text-title-contrast">
          <legend class="uppercase text-heading-3xl font-display">
            Select a page type
          </legend>
          <p>Create a new page by using one of the following options</p>
        </div>
        <fieldset class="grid grid-cols-1 md:grid-cols-2 gap-2.5">
          <PageSelector>
            <input
              type="radio"
              id="LandingPageInput"
              value="ConfigurablePage"
              name="pageSelection"
              aria-describedby="LandingPageDefinition"
              class="sr-only"
              v-model="pageType"
            />
            <div class="group">
              <label for="LandingPageInput" class="hover:cursor-pointer">
                <BaseIcon name="Docs" :width="32" />
                <span class="font-bold">Landing page</span>
                <p id="LandingPageDefinition">
                  Create a generic page to display general information such as
                  an contact page or a home page.
                </p>
              </label>
            </div>
          </PageSelector>
          <PageSelector>
            <input
              type="radio"
              id="DeveloperPageInput"
              value="DeveloperPage"
              name="pageSelection"
              aria-describedby="DeveloperPageDefinition"
              class="sr-only"
              v-model="pageType"
            />
            <label for="DeveloperPageInput" class="hover:cursor-pointer">
              <BaseIcon name="CodeBlocks" :width="32" />
              <span class="font-bold">Developer page</span>
              <p id="DeveloperPageDefinition">
                Build your own page from scratch using HTML, CSS, and
                JavaScript.
              </p>
            </label>
          </PageSelector>
        </fieldset>
      </form>
    </div>
    <template #footer>
      <div class="flex justify-between items-center flex-none h-modal-footer">
        <ul class="flex items-center justify-end w-full gap-4">
          <li>
            <Button type="primary" @click="onCreatePage"> Create page </Button>
          </li>
        </ul>
      </div>
    </template>
  </Modal>
  <EditModal
    v-if="formMetadata && enableEditing"
    key="edit-modal-configurable-page"
    :showButton="false"
    :schemaId="(schema as string)"
    :metadata="formMetadata"
    :formValues="formValues"
    :isInsert="true"
    v-model:visible="showFormModal"
    @update:cancelled="onClose"
    @update:addedFormValues="onAddFormValues"
  />
</template>
