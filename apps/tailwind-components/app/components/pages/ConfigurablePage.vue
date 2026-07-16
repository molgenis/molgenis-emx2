<script setup lang="ts">
import type { IConfigurablePages } from "../../../types/cms";
import type { ITableMetaData } from "../../../../metadata-utils/src";

import PageComponent from "./PageComponent.vue";
import TextParagraph from "./Paragraph.vue";
import AddComponentPalette from "./AddComponentPalette.vue";
import ComponentDropZone from "./ComponentDropZone.vue";
import { ref } from "vue";

const props = withDefaults(
  defineProps<{
    content: IConfigurablePages;
    isEditable: boolean;
    metadata: ITableMetaData[];
    schema: string;
  }>(),
  {
    isEditable: false,
  }
);

const emit = defineEmits(["updatePage"]);
const handleDragEvent = (value: any) => {
  draggingInfo.value = value;
};
const draggingInfo = ref<{
  dragging: boolean;
  componentName: string;
  componentType: string;
}>({ dragging: false, componentName: "", componentType: "" });
</script>

<template>
  <div>
    <template v-for="orderedBlock in content.blockOrder" :key="orderedBlock.id">
      <PageComponent
        v-if="orderedBlock.block.mg_tableclass.endsWith('.Headers')"
        :mg_tableclass="orderedBlock.block.mg_tableclass"
        :component="orderedBlock.block"
        :is-editable="isEditable"
        :metadata="metadata"
      />
      <PageComponent
        v-else-if="orderedBlock.block.mg_tableclass.endsWith('.Sections')"
        :mg_tableclass="orderedBlock.block.mg_tableclass"
        :component="orderedBlock.block"
        @update-page="$emit('updatePage')"
      >
        <ComponentDropZone
          v-if="isEditable"
          :draggingInfo="draggingInfo"
          :schema="schema"
          :order="
            orderedBlock.block?.componentOrder
              ? orderedBlock.block.componentOrder[0].order
              : orderedBlock.block.order
          "
          :parent="orderedBlock.block.id"
          componentType="Component"
          @update-page="$emit('updatePage')"
        />
        <template
          v-for="orderedComponent in orderedBlock.block.componentOrder"
          :key="orderedComponent.id"
        >
          <PageComponent
            :mg_tableclass="orderedComponent.component.mg_tableclass"
            :component="orderedComponent.component"
            :is-editable="isEditable"
            :metadata="metadata"
            @update-page="$emit('updatePage')"
          />
          <ComponentDropZone
            v-if="isEditable"
            :draggingInfo="draggingInfo"
            :schema="schema"
            :order="orderedComponent.order + 1"
            :parent="orderedBlock.block.id"
            componentType="Component"
            @update-page="$emit('updatePage')"
          />
        </template>
      </PageComponent>
      <TextParagraph
        v-else
        id="block-does-not-exist-message"
        :text="`Block ${orderedBlock.block.mg_tableclass} is not yet supported.`"
      />
      <ComponentDropZone
        v-if="isEditable"
        :pageName="content.name"
        :draggingInfo="draggingInfo"
        :schema="schema"
        :order="orderedBlock.order ? orderedBlock.order + 1 : 0"
        :parent="content.name"
        componentType="Block"
        @update-page="$emit('updatePage')"
      />
    </template>
    <AddComponentPalette
      v-if="isEditable"
      @dragging="handleDragEvent"
      :content="content"
      :metadata="metadata"
    />
  </div>
</template>
