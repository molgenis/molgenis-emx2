<script setup lang="ts">
import type {
  ICollection,
  INameObject,
  IDefinitionListItem,
} from "~/interfaces/types";

import dateUtils from "~/utils/dateUtils";

const props = defineProps<{
  title: string;
  description?: string;
  collection: ICollection;
}>();

const generalDesign: IDefinitionListItem[] = [
  {
    label: "Type",
    content: props.collection.type
      ? props.collection.type.map((type: INameObject) => type?.name).join(", ")
      : undefined,
  },
  {
    label: "collection type",
    content: props.collection.type
      ? props.collection.type.map((type: INameObject) => type?.name).join(", ")
      : undefined,
  },
  {
    label: "Design",
    content:
      props.collection.design?.definition && props.collection.design?.name
        ? {
            value: props.collection.design?.name,
            tooltip: props.collection.design?.definition,
          }
        : props.collection.design?.name,
  },
  {
    label: "Design description",
    content: props.collection.designDescription,
  },
  {
    label: "Design schematic",
    content: props.collection.designSchematic,
  },
  {
    label: "Collection type",
    content: props.collection.collectionType
      ? props.collection.collectionType
          .map((collectionType) => collectionType.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Start/End collection",
    content: dateUtils.startEndYear(
      props.collection.startDate,
      props.collection.endDate
    ),
  },
  {
    label:
      props.collection.designPaper && props.collection.designPaper?.length > 1
        ? "Design papers"
        : "Design paper",
    type: "LINK",
    content: props.collection.designPaper
      ? designPaperToItem(props.collection.designPaper)
      : undefined,
  },
  {
    label: "PID",
    content: props.collection.pid,
  },
];

function designPaperToItem(designPaper: any[]) {
  if (designPaper.length === 0) {
    return undefined;
  } else if (designPaper.length === 1) {
    return {
      type: "LINK",
      label: designPaper[0].title,
      url: designPaper[0].doi,
    };
  } else if (designPaper.length > 1)
    return designPaper.map((paper) => ({
      type: "LINK",
      label: paper.title,
      url: paper.doi,
    }));
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <CatalogueItemList
      :items="generalDesign.filter((item) => item.content !== undefined)"
    />
  </ContentBlock>
</template>
