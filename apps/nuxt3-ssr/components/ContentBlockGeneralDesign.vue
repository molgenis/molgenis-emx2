<script setup lang="ts">
const { cohort } = defineProps<{
  title: string;
  description?: string;
  cohort: ICohort;
}>();

let generalDesign: { label: String; content: any; tooltip?: string }[] = [];
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
        : "not available",
    },
    {
      label: "Design",
      content: cohort?.design ? cohort?.design.name : "not available",
      tooltip: cohort?.design?.definition
        ? cohort?.design?.definition
        : "not available",
    },
    {
      label: "Collection type",
      content: cohort?.collectionType
        ? cohort?.collectionType[0].name
        : "not available",
    },
    {
      label: "Start/End year",
      content: `${cohort?.startYear} - ${cohort?.endYear}`,
    },
    {
      label: "Population",
      content: cohort?.countries
        ? [...cohort?.countries]
            .sort((a, b) => a.order - b.order)
            .map((c) => c.name)
            .join(", ")
        : "",
    },
    {
      label: "Number of participants",
      content: cohort?.numberOfParticipants,
    },
    {
      label: "Age group at inclusion",
      content: cohort?.populationAgeGroups
        ? cohort?.populationAgeGroups.map((pag) => pag.name)
        : [],
    },
  ];
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <DefinitionList :items="generalDesign" />
  </ContentBlock>
</template>
