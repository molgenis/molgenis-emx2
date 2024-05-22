<script setup lang="ts">
import type {
  ICohort,
  INameObject,
  IDefinitionListItem,
} from "~/interfaces/types";

import dateUtils from "~/utils/dateUtils";

const props = defineProps<{
  title: string;
  description?: string;
  cohort: ICohort;
}>();

const generalDesign: IDefinitionListItem[] = [
  {
    label: "Cohort type",
    content: props.cohort.type
      ? props.cohort.type.map((type: INameObject) => type?.name).join(", ")
      : undefined,
  },
  {
    label: "Design",
    content:
      props.cohort.design?.definition && props.cohort.design?.name
        ? {
            value: props.cohort.design?.name,
            tooltip: props.cohort.design?.definition,
          }
        : props.cohort.design?.name,
  },
  {
    label: "Design description",
    content: props.cohort.designDescription,
  },
  {
    label: "Design schematic",
    content: props.cohort.designSchematic,
  },
  {
    label: "Collection type",
    content: props.cohort.collectionType
      ? props.cohort.collectionType
          .map((collectionType) => collectionType.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Start/End year",
    content: dateUtils.startEndYear(
      props.cohort.startYear,
      props.cohort.endYear
    ),
  },
  {
    label:
      props.cohort.designPaper && props.cohort.designPaper?.length > 1
        ? "Design papers"
        : "Design paper",
    type: "LINK",
    content: props.cohort.designPaper
      ? designPaperToItem(props.cohort.designPaper)
      : undefined,
  },
  {
    label: "PID",
    content: props.cohort.pid,
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
