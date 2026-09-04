<script setup lang="ts">
import { ref } from "vue";

import PageGalleryCardAction from "./PageGalleryCardAction.vue";
import BaseIcon from "../../BaseIcon.vue";

import type { IContainers } from "../../../../types/cms.ts";
import type { ICmsPageTypes } from "../../../../types/CmsComponents.ts";

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

const emits = defineEmits<{
  (e: "deleted", value: boolean): void;
}>();

const currentPageType = ref<string | undefined>(
  setCmsPageType(props.container.mg_tableclass)
);

async function deletePage() {
  const pageTableClass = props.container.mg_tableclass as ICmsPageTypes;
  const pageName = props.container.name;
  console.log(`Deleting: ${pageName} (${pageTableClass})`);

  if (pageTableClass.endsWith(".Developer pages")) {
    await deleteDeveloperPage(props.schema, pageName);
    emits("deleted", true);
  } else if (pageTableClass.endsWith(".Configurable pages")) {
    console.log("Configurable page clicked");
    await deleteConfigurablePage(props.schema, pageName);
    emits("deleted", true);
  } else {
    console.log("Page type", pageTableClass, "not defined");
    return undefined;
  }
}
</script>

<template>
  <div
    :id="container.name"
    class="relative group border rounded-base w-full h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center text-title-contrast"
  >
    <NuxtLink
      :to="setCmsViewUrl(schema, container.name)"
      class="hover:underline"
    >
      {{ container.name }}
    </NuxtLink>
    <div
      class="absolute bottom-0 w-full p-2 flex items-center justify-between flex-row gap-2.5 bg-form-legend"
    >
      <div class="w-auto">
        <span class="ml-2.5 font-display text-body-sm" v-if="currentPageType">
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
            class="hover:underline cursor-pointer"
          >
            <BaseIcon name="Edit" :width="18" />
            <span class="sr-only">edit page</span>
          </NuxtLink>
        </PageGalleryCardAction>
        <PageGalleryCardAction
          class="p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
          v-tooltip.bottom="`Delete`"
        >
          <button id="deletePage" @click="deletePage">
            <BaseIcon name="Trash" :width="18" />
            <span class="sr-only">delete page</span>
          </button>
        </PageGalleryCardAction>
      </div>
    </div>
  </div>
</template>
