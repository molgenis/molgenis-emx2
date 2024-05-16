<script setup lang="ts">
import type {
  ICohort,
  INameObject,
  DefinitionListItemType,
  IDefinitionListItem,
} from "~/interfaces/types";
import type { IOntologyItem } from "meta-data-utils";
import dateUtils from "~/utils/dateUtils";

const props = defineProps<{
  title: string;
  description?: string;
  cohort: ICohort;
  mainMedicalCondition?: IOntologyItem[];
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

const population: IDefinitionListItem[] = [
  {
    label: "Countries",
    content: props.cohort?.countries
      ? [...props.cohort?.countries]
          .sort((a, b) => b.order - a.order)
          .map((country) => country.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Regions",
    content: props.cohort?.regions
      ?.sort((a, b) => b.order - a.order)
      .map((r) => r.name)
      .join(", "),
  },
  {
    label: "Number of participants",
    content: props.cohort?.numberOfParticipants,
  },
  {
    label: "Number of participants with samples",
    content: props.cohort?.numberOfParticipantsWithSamples,
  },
  {
    label: "Age group at inclusion",
    content: removeChildIfParentSelected(
      props.cohort?.populationAgeGroups || []
    )
      .sort((a, b) => a.order - b.order)
      .map((ageGroup) => ageGroup.name)
      .join(", "),
  },
  {
    label: "Population oncology topology",
    type: "ONTOLOGY",
    content: props.cohort.populationOncologyTopology,
  },
  {
    label: "Population oncology morphology",
    type: "ONTOLOGY",
    content: props.cohort.populationOncologyMorphology,
  },
  {
    label: "Inclusion criteria",
    type: "ONTOLOGY",
    content: props.cohort.inclusionCriteria,
  },
  {
    label: "Other inclusion criteria",
    content: props.cohort.otherInclusionCriteria,
  },
];

if (props.mainMedicalCondition && props.mainMedicalCondition.length > 0) {
  population.splice(population.length - 4, 0, {
    label: "Main medical condition",
    content: props.mainMedicalCondition,
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
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <CatalogueItemList
      :items="generalDesign.filter((item) => item.content !== undefined)"
    />

    <h2 class="my-5 uppercase text-heading-4xl font-display">Population</h2>

    <CatalogueItemList
      :items="population.filter((item) => item.content !== undefined)"
    />
  </ContentBlock>
</template>
