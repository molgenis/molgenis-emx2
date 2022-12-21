<script setup lang="ts">
const { cohort } = defineProps<{
  title: string;
  description?: string;
  cohort: ICohort;
}>();

let generalDesign: { label: String; content: any; tooltip?: string, type?: "ONTOLOGY" }[] = [];
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
      content: cohort?.design ? cohort?.design.name : undefined,
    },
    {
      label: "Design definition",
      content: cohort?.design?.definition
    },
    {
      label: "Design description",
      content: cohort?.designDescription
    },
    {
      label: "Collection type",
      content: cohort?.collectionType
        ? cohort?.collectionType[0].name
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
          .sort((a, b) => a.order - b.order)
          .map((c) => c.name)
          .join(", ")
        : undefined,
    },
    {
      label: "Regions",
      content: cohort?.regions?.map(r => r.name).join(", ")
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
      content: buildOntologyTree(cohort?.populationAgeGroups),
      type: "ONTOLOGY"
    },
    {
      label: "Inclusion criteria",
      content: cohort?.inclusionCriteria
    },
    {
      label: "Marker paper",
      content: cohort?.designPaper?.map(dp => {
        return dp.title + (dp.doi ? ` (doi: ${dp.doi})` : '')
      })
    },
  ];
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <DefinitionList :items="generalDesign.filter(item => item.content !== undefined)" />
  </ContentBlock>
</template>
