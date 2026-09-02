<script setup lang="ts">
import { ref } from "vue";
import {
  setCmsViewUrl,
  setCmsEditorUrl,
  setCmsPageType,
  getPage,
  getPageComponents,
  updateBlocks,
  deleteBlock,
  deleteContainer,
} from "../../../utils/cms.ts";
import type {
  IContainers,
  IConfigurablePages,
  IDeveloperPages,
  IBlocks,
} from "../../../../types/cms.ts";
import PageGalleryCardAction from "./PageGalleryCardAction.vue";

import BaseIcon from "../../BaseIcon.vue";

interface IBlockIds {
  blockId: string;
  blockOrderId: string;
}

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

const currentPageType = ref<string | undefined>(
  setCmsPageType(props.container.mg_tableclass)
);

async function deletePage() {
  if (props.container.mg_tableclass?.endsWith(".Configurable pages")) {
    // retrieve page metadata
    const pageData = await getPage(props.schema, props.container.name);
    const page = pageData.page as IConfigurablePages;
    console.log("Page:", page);

    // remove multiple links of components that are used in more than one place
    const components = await getPageComponents(
      props.schema,
      props.container.name
    );
    const currentPageBlocks = page.blocks?.map((block) => block.id);
    if (components && currentPageBlocks) {
      const componentsInMultipleBlocks = components
        .filter((component) => {
          return component.inBlock && component.inBlock.length > 1;
        })
        .map((component) => {
          const newComponent = component;
          const revisedComponents = newComponent.inBlock.filter(
            (block: IBlocks) => {
              return !currentPageBlocks?.includes(block.id);
            }
          );
          newComponent.inBlock = revisedComponents;
          return newComponent;
        });
      console.log(
        currentPageBlocks,
        "\n updates:",
        componentsInMultipleBlocks,
        "\n components:",
        components
      );
    }

    // find blocks that are also used in other pages
    if (page.blocks) {
      const blocksInMultiplePages = page.blocks
        ?.filter((block) => {
          return block.inContainer && block.inContainer.length > 1;
        })
        .map((block) => {
          const newBlock = block;
          const revisedContainers = newBlock.inContainer.filter(
            (container: any) => {
              return container.name !== props.container.name;
            }
          );
          newBlock.inContainer = revisedContainers;
          return newBlock;
        }) as IBlocks[];

      // remove link to current block if multiple exist
      if (blocksInMultiplePages) {
        // console.log("remove blocks:", blocksInMultiplePages);
        // await updateBlocks(props.schema, blocksInMultiplePages);
      }
    }

    // get blocks
    const pageBlocks = page.blockOrder?.map((block) => {
      return { blockId: block.block.id, blockOrderId: block.id };
    }) as IBlockIds[];
    for (const block of pageBlocks) {
      // await deleteBlock(props.schema, block.blockId, block.blockOrderId, props.container.name);
      // await deleteContainer(props.schema, props.container.name);
    }
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
