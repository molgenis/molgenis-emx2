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
const formType = ref<ICmsPageTypes | undefined>();
const showFormModal = ref<boolean>(false);
const showPageDropdown = ref<boolean>(false);

const { isAdmin, session } = await useSession(schema);
const enableEditing = computed(() => {
  return (
    session.value?.roles?.[schema as string]?.includes("Manager") ||
    isAdmin.value
  );
});

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

function onAddNewPageClick(type: ICmsPageTypes) {
  formValues.value = null;
  showPageDropdown.value = false;
  formType.value = type;
  if (formType.value === "ConfigurablePage") {
    formMetadata.value = data.value?.configurablePageMetadata;
  } else {
    formMetadata.value = data.value?.developerPageMetadata;
    const newPage = newDeveloperPage(
      "<h2>My new page</h2>\n<p>This is a demo page</p>"
    );
    formValues.value = newPage;
  }
  showFormModal.value = true;
}

async function onClose() {
  showFormModal.value = false;
  formMetadata.value = undefined;
  formValues.value = undefined;
  formType.value = undefined;
  await refresh();
}

async function onAddFormValues(value: IContainers) {
  if (formType.value === "ConfigurablePage" && value.name) {
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
        <div class="relative" v-if="enableEditing">
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
              Landing page
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
