<script setup lang="ts">
import { Ref } from "vue";
import subcohortGql from "~~/gql/subcohort";
import ContentBlockModal from "./content/ContentBlockModal.vue";
const config = useRuntimeConfig();
const route = useRoute();

const { id } = defineProps<{
  id: string;
}>();

const query = moduleToString(subcohortGql);

let subcohort: Ref = ref();
const { data: subcohortData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: query,
      variables: { id: route.params.cohort, name: id },
    },
  }
).catch((e) => console.log(e));

watch(
  subcohortData,
  function setData(data: any) {
    subcohort = data?.data?.Subcohorts[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

function renderList(
  list: any[],
  itemMapper: (a: any) => string,
  itemJoiner?: (a: any) => string
) {
  if (list?.length === 1) {
    return itemMapper(list[0]);
  }

  const mapped = list.map(itemMapper);
  if (!itemJoiner) {
    return mapped;
  }

  return itemJoiner(mapped);
}

const toName = (item: any) => item.name;
const toCommaList = (items: any) => items.join(",");

const items: any = [];

if (subcohort?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subcohort.numberOfParticipants,
  });
}

if (subcohort?.inclusionStart || subcohort?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: filters.startEndYear(
      subcohort.inclusionStart,
      subcohort.inclusionEnd
    ),
  });
}

if (subcohort?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(
      removeChildIfParentSelected(subcohort.ageGroups),
      toName
    ),
  });
}

if (subcohort?.mainMedicalCondition) {
  items.push({
    label: "Main medical condition",
    type: "ONTOLOGY",
    content: buildOntologyTree(subcohort.mainMedicalCondition),
  });
}

if (subcohort?.comorbidity) {
  items.push({
    label: "Comorbidity",
    type: "ONTOLOGY",
    content: buildOntologyTree(subcohort.comorbidity),
  });
}

if (subcohort?.countries) {
  items.push({
    label: "Population",
    type: "ONTOLOGY",
    content: buildOntologyTree(subcohort.countries),
  });
}

if (subcohort?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subcohort.inclusionCriteria,
  });
}

// todo add count table ( empty in current test set)
</script>

<template>
  <ContentBlockModal
    v-if="subcohort"
    :title="subcohort?.name"
    :description="subcohort?.description"
  >
    <DefinitionList :items="items" :small="true" />
  </ContentBlockModal>
</template>
