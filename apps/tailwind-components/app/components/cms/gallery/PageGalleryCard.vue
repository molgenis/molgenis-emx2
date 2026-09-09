<script setup lang="ts">
import { ref } from "vue";

import Modal from "../../Modal.vue";
import PageGalleryCardAction from "./PageGalleryCardAction.vue";
import BaseIcon from "../../BaseIcon.vue";

import type { IContainers } from "../../../../types/cms.ts";
import type {
  ICmsPageTypes,
  IDeleteContainerStatus,
} from "../../../../types/CmsComponents.ts";

import {
  setCmsViewUrl,
  setCmsEditorUrl,
  setCmsPageType,
} from "../../../utils/cms.ts";
import {
  deleteDeveloperPage,
  deleteConfigurablePage,
} from "../../../utils/cms/delete.ts";

const props = withDefaults(
  defineProps<{
    isEditable?: boolean;
    schema: string;
    container: IContainers;
  }>(),
  {
    isEditable: false,
  }
);

const showDeleteModal = ref<boolean>(false);

const emits = defineEmits<{
  (e: "deleted", value: IDeleteContainerStatus): void;
}>();

const currentPageType = ref<string | undefined>(
  setCmsPageType(props.container.mg_tableclass)
);

async function deletePage() {
  showDeleteModal.value = false;
  const pageTableClass = props.container.mg_tableclass as ICmsPageTypes;
  const pageName = props.container.name;

  if (pageTableClass.endsWith(".Developer pages")) {
    const result = await deleteDeveloperPage(props.schema, pageName);
    emits("deleted", result);
  } else if (pageTableClass.endsWith(".Configurable pages")) {
    const result = await deleteConfigurablePage(props.schema, pageName);
    emits("deleted", result);
  } else {
    return undefined;
  }
}
</script>

<template>
  <div
    :id="container.name"
    class="relative group border rounded-base w-full hover:shadow-md transition-shadow text-title-contrast"
  >
    <div class="h-32 flex items-center justify-center text-center p-7.5">
      <NuxtLink
        :to="setCmsViewUrl(schema, container.name)"
        class="hover:underline"
      >
        {{ container.name }}
      </NuxtLink>
    </div>
    <div
      class="flex items-center justify-between flex-row gap-2.5 p-2.5 bg-form-legend"
    >
      <div class="w-auto">
        <span class="ml-2.5 font-display text-body-base" v-if="currentPageType">
          {{ currentPageType }}
        </span>
      </div>
      <div
        v-if="isEditable"
        class="flex flex-row gap-2.5"
        role="toolbar"
        :aria-controls="container.name"
      >
        <PageGalleryCardAction v-tooltip.bottom="`Edit`">
          <NuxtLink
            :to="setCmsEditorUrl(schema, (container.mg_tableclass as string), container.name)"
            class="hover:underline cursor-pointer h-10 w-10 p-2.5"
          >
            <BaseIcon name="Edit" :width="18" />
            <span class="sr-only">edit page</span>
          </NuxtLink>
        </PageGalleryCardAction>
        <PageGalleryCardAction v-tooltip.bottom="`Delete`">
          <button
            id="deletePage"
            @click="showDeleteModal = true"
            class="hover:underline cursor-pointer h-10 w-10 p-2.5"
          >
            <BaseIcon name="Trash" :width="18" />
            <span class="sr-only">delete page</span>
          </button>
        </PageGalleryCardAction>
      </div>
    </div>
  </div>
  <Modal
    v-model:visible="showDeleteModal"
    title="Delete page"
    size="small"
    @closed="showDeleteModal = false"
  >
    <div class="min-h-0 p-12.5 text-title-contrast">
      <p>Are you sure you want to delete the page "{{ container.name }}"?</p>
    </div>
    <template #footer>
      <div class="flex justify-between items-center flex-none h-modal-footer">
        <ul class="flex items-center justify-end w-full gap-4">
          <li>
            <Button type="secondary" @click="showDeleteModal = false">
              Cancel
            </Button>
          </li>
          <li>
            <Button type="primary" @click="deletePage"> Delete </Button>
          </li>
        </ul>
      </div>
    </template>
  </Modal>
</template>
