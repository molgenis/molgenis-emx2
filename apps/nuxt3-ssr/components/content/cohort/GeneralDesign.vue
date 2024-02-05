<script setup lang="ts">
import type { ICohort, INameObject } from "~/interfaces/types";
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
  type?: "ONTOLOGY";
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
      label: "Marker paper",
      content: cohort?.designPaper?.map((dp) => {
        return dp.title + (dp.doi ? ` (doi: ${dp.doi})` : "");
      }),
    },
    {
      label: "Main medical condition",
      content: mainMedicalCondition,
      type: "ONTOLOGY",
    },
    {
      label: "PID",
      content: cohort?.pid,
    },
  ];
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <CatalogueItemList
      :items="generalDesign.filter((item) => item.content !== undefined)"
    />
  </ContentBlock>
</template>
