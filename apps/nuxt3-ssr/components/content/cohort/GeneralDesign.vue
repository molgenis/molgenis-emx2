<script setup lang="ts">
import type {
  IResource,
  INameObject,
  IDefinitionListItem,
} from "~/interfaces/types";

import dateUtils from "~/utils/dateUtils";

const props = defineProps<{
  title: string;
  description?: string;
  resource: IResource;
}>();

const designPublications = computed(() =>
  props.resource.publications?.filter((p) => p.isDesignPublication)
);

const generalDesign: IDefinitionListItem[] = [
  {
    label: "Type",
    content: props.resource.type
      ? props.resource.type.map((type: INameObject) => type?.name).join(", ")
      : undefined,
  },
  {
    label: "Type other",
    content: props.resource.typeOther ? props.resource.typeOther : undefined,
  },
  {
    label: "Cohort type",
    content: props.resource.cohortType
      ? props.resource.cohortType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "RWD type",
    content: props.resource.rWDType
      ? props.resource.rWDType.map((type: INameObject) => type?.name).join(", ")
      : undefined,
  },
  {
    label: "Network type",
    content: props.resource.networkType
      ? props.resource.networkType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Clinical study type",
    content: props.resource.clinicalStudyType
      ? props.resource.clinicalStudyType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Data collection type",
    content: props.resource.dataCollectionType
      ? props.resource.dataCollectionType
          .map((type: INameObject) => type?.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Data collection description",
    content: props.resource.dataCollectionDescription,
  },
  {
    label: "Design",
    content:
      props.resource.design?.definition && props.resource.design?.name
        ? {
            value: props.resource.design?.name,
            tooltip: props.resource.design?.definition,
          }
        : props.resource.design?.name,
  },
  {
    label: "Design description",
    content: props.resource.designDescription,
  },
  {
    label: "Design schematic",
    content: props.resource.designSchematic,
  },
  {
    label: "Reason sustained",
    content: props.resource.reasonSustained,
  },
  {
    label: "Record trigger",
    content: props.resource.recordTrigger,
  },
  {
    label: "Unit of observation",
    content: props.resource.unitOfObservation,
  },
  {
    label: "Keywords",
    content: props.resource.keywords,
  },
  {
    label: "Date established",
    content: props.resource.dateEstablished,
  },
  {
    label: "Start/End data collection",
    content: dateUtils.startEndYear(
      props.resource.startDataCollection,
      props.resource.endDataCollection
    ),
  },
  {
    label:
      designPublications.value?.length > 1 ? "Design papers" : "Design paper",
    type: "LINK",
    content: designPublications.value
      ? designPaperToItem(designPublications.value)
      : undefined,
  },
  {
    label: "PID",
    content: props.resource.pid,
  },
  {
    label: "External identifiers",
    content: props.resource.externalIdentifiers
      ? props.resource.externalIdentifiers
          .map((id) => id.externalIdentifierType.name + ": " + id.identifier)
          .join(", ")
      : undefined,
  },
  {
    label: "License",
    content: props.resource.license
      ? { url: props.resource.license, label: props.resource.license }
      : undefined,
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
