<template>
  <DefinitionListTerm>
    <div class="flex items-center gap-1">
      {{ item.label }}
      <div v-if="item.tooltip">
        <CustomTooltip label="Read more" :content="item.tooltip" />
      </div>
    </div>
  </DefinitionListTerm>

  <DefinitionListDefinition :small="small">
    <ContentOntology
      v-if="item?.type === 'ONTOLOGY'"
      :tree="buildTree(item.content)"
      :collapse-all="true"
      :inverted="true"
    ></ContentOntology>

    <ContentMapped
      v-else-if="item?.type === 'MAPPED'"
      :mapped="item.content"
    ></ContentMapped>

    <ul
      v-else-if="isArray(item.content) && item.content.length > 1"
      class="grid gap-1 pl-4 list-disc list-outside"
    >
      <li v-for="row in item.content" :key="row">
        <a
          v-if="row.type === 'LINK'"
          class="text-blue-500 text-body-base hover:underline"
          target="_blank"
          :href="row.url"
          >{{ row.label }}</a
        >
        <span v-else>
          {{ row }}
        </span>
      </li>
    </ul>

    <a
      v-else-if="item.type === 'LINK'"
      class="text-blue-500 text-body-base hover:underline"
      target="_blank"
      :href="item.content.url"
    >
      {{ item.content.label }}</a
    >

    <a v-else-if="showAsFile(item)" class="flex" :href="item.content.url">
      <div class="flex-start">
        <span class="text-blue-500 text-body-base hover:underline">
          {{ item.label }}
        </span>
      </div>
    </a>

    <div v-else-if="item?.content?.tooltip" class="flex items-center gap-1">
      {{ item.content.value }}
      <CustomTooltip label="Read more" :content="item.content.tooltip" />
    </div>
    <div v-else>
      {{ Array.isArray(item.content) ? item.content[0] : item.content }}
    </div>
  </DefinitionListDefinition>
</template>

<script setup lang="ts">
import type { IDefinitionListItem } from "~/interfaces/types";
import { buildTree } from "../utils/ontologyUtils";

withDefaults(
  defineProps<{
    item: IDefinitionListItem;
    small?: boolean;
  }>(),
  {
    small: false,
  }
);

const isArray = (value: []) => {
  return Array.isArray(value);
};

function showAsFile(item: IDefinitionListItem) {
  return item.content && item.content.url && item.content.extension;
}
</script>
