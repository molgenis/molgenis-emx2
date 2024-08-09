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
    label: "Type other",
    content: props.collection.typeOther
      ? props.collection.typeOther
      : undefined,
  },
  {
    label: "Cohort type",
    content: props.collection.cohortType
      ? props.collection.cohortType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "RWD type",
    content: props.collection.rWDType
      ? props.collection.rWDType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Network type",
    content: props.collection.networkType
      ? props.collection.networkType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Clinical study type",
    content: props.collection.clinicalStudyType
      ? props.collection.clinicalStudyType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Data collection type",
    content: props.collection.dataCollectionType
      ? props.collection.dataCollectionType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Data collection description",
    content: props.collection.dataCollectionDescription,
  },
  {
    label: "Design",
    content:
      props.collection.designType?.definition &&
      props.collection.designType?.name
        ? {
            value: props.collection.designType?.name,
            tooltip: props.collection.designType?.definition,
          }
        : props.collection.designType?.name,
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
    label: "Reason sustained",
    content: props.collection.reasonSustained,
  },
  {
    label: "Record trigger",
    content: props.collection.recordTrigger,
  },
  {
    label: "Unit of observation",
    content: props.collection.unitOfObservation,
  },
  {
    label: "Keywords",
    content: props.collection.keywords,
  },
  {
    label: "Date established",
    content: props.collection.dateEstablished,
  },
  {
    label: "Start/End data collection",
    content: dateUtils.startEndYear(
      props.collection.startDataCollection,
      props.collection.endDataCollection
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
  {
    label: "External identifiers",
    content: props.collection.externalIdentifiers
      ? props.collection.externalIdentifiers
          .map((id) => id.externalIdentifierType.name + ": " + id.identifier)
          .join(", ")
      : undefined,
  },
  {
    label: "License",
    content: { url: props.collection.license, label: props.collection.license },
    type: "LINK",
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
