<script setup lang="ts">
import type {
  DefinitionListItemType,
  ICohort,
  INameObject,
} from "~/interfaces/types";
import type { IOntologyItem } from "meta-data-utils";

const { cohort, mainMedicalCondition } = defineProps<{
  title: string;
  description?: string;
  cohort: ICohort;
  mainMedicalCondition?: IOntologyItem[];
}>();

let generalDesign: {
  label: string;
  content: any;
  tooltip?: string;
  type?: DefinitionListItemType;
}[] = [];
watch(cohort, setData, {
  deep: true,
  immediate: true,
});

function setData() {
  generalDesign = [
    {
      label: "Cohort type",
      content: cohort?.type
        ? cohort?.type.map((type: INameObject) => type?.name).join(", ")
        : undefined,
    },
    {
      label: "Design",
      content:
        cohort?.design?.definition && cohort?.design?.name
          ? {
              value: cohort?.design?.name,
              tooltip: cohort?.design?.definition,
            }
          : cohort?.design?.name,
    },
    {
      label: "Design description",
      content: cohort?.designDescription,
    },
    {
      label: "Design schematic",
      content: cohort?.designSchematic,
    },
    {
      label: "Collection type",
      content: cohort?.collectionType
        ? cohort?.collectionType
            .map((collectionType) => collectionType.name)
            .join(", ")
        : undefined,
    },
    {
      label: "Start/End year",
      content: filters.startEndYear(cohort?.startYear, cohort?.endYear),
    },
    {
      label: "Population",
      content: cohort?.countries
        ? [...cohort?.countries]
            .sort((a, b) => b.order - a.order)
            .map((country) => country.name)
            .join(", ")
        : undefined,
    },
    {
      label: "Regions",
      content: cohort?.regions
        ?.sort((a, b) => b.order - a.order)
        .map((r) => r.name)
        .join(", "),
    },
    {
      label: "Number of participants",
      content: cohort?.numberOfParticipants,
    },
    {
      label: "Number of participants with samples",
      content: cohort?.numberOfParticipantsWithSamples,
    },
    {
      label: "Age group at inclusion",
      content: removeChildIfParentSelected(cohort?.populationAgeGroups || [])
        .sort((a, b) => a.order - b.order)
        .map((ageGroup) => ageGroup.name)
        .join(", "),
    },
    {
      label: "Inclusion criteria",
      content: cohort?.otherInclusionCriteria,
    },
    {
      label:
        cohort?.designPaper && cohort.designPaper?.length > 1
          ? "Marker papers"
          : "Marker paper",
      type: "LINK",
      content: cohort?.designPaper
        ? designPaperToItem(cohort?.designPaper)
        : undefined,
    },
    {
      label: "PID",
      content: cohort?.pid,
    },
  ];

  if (mainMedicalCondition && mainMedicalCondition.length > 0) {
    generalDesign.splice(generalDesign.length - 2, 0, {
      label: "Main medical condition",
      content: mainMedicalCondition,
      type: "ONTOLOGY",
    });
  }

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
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <CatalogueItemList
      :items="generalDesign.filter((item) => item.content !== undefined)"
    />
  </ContentBlock>
</template>
