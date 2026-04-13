<script setup lang="ts">
import type { IConfigurablePages } from "../../../types/cms";
import type { ITableMetaData } from "../../../../metadata-utils/src";

import PageComponent from "./PageComponent.vue";
import TextParagraph from "./Paragraph.vue";

const props = withDefaults(
  defineProps<{
    content: IConfigurablePages;
    isEditable: boolean;
    metadata: ITableMetaData[];
  }>(),
  {
    isEditable: false,
  }
);
</script>

<template>
  <template v-for="orderedBlock in content.blockOrder" :key="orderedBlock.id">
    <PageComponent
      v-if="orderedBlock.block.mg_tableclass === 'cms.Headers'"
      :mg_tableclass="orderedBlock.block.mg_tableclass"
      :component="orderedBlock.block"
      :is-editable="isEditable"
      :metadata="metadata"
    />
    <PageComponent
      v-else-if="orderedBlock.block.mg_tableclass === 'cms.Sections'"
      :mg_tableclass="orderedBlock.block.mg_tableclass"
      :component="orderedBlock.block"
    >
      <template
        v-for="orderedComponent in orderedBlock.block.componentOrder"
        :key="orderedComponent.id"
      >
        <PageComponent
          :mg_tableclass="orderedComponent.component.mg_tableclass"
          :component="orderedComponent.component"
          :is-editable="isEditable"
          :metadata="metadata"
        />
      </template>
    </PageComponent>
    <TextParagraph
      v-else
      id="block-does-not-exist-message"
      :text="`Block ${orderedBlock.block.mg_tableclass} is not yet supported.`"
    />
  </template>
</template>
