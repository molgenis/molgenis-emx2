<script setup lang="ts">
import cohortGql from "~~/gql/cohort";
import dateUtils from "~/utils/dateUtils";
const route = useRoute();

const { id } = defineProps<{
  id: string;
}>();

const query = moduleToString(cohortGql);

let cohort: Ref = ref();
const { data: cohortData } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: {
    query: query,
    variables: { id: route.params.resource, name: id },
  },
}).catch((e) => console.log(e));

watch(
  cohortData,
  function setData(data: any) {
    cohort = data?.data?.ResourceCohorts[0];
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

if (cohort?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: cohort.numberOfParticipants,
  });
}

if (cohort?.inclusionStart || cohort?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(cohort.inclusionStart, cohort.inclusionEnd),
  });
}

if (cohort?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(removeChildIfParentSelected(cohort.ageGroups), toName),
  });
}

if (cohort?.mainMedicalCondition) {
  items.push({
    label: "Main medical condition",
    type: "ONTOLOGY",
    content: cohort.mainMedicalCondition,
  });
}

if (cohort?.comorbidity) {
  items.push({
    label: "Comorbidity",
    type: "ONTOLOGY",
    content: cohort.comorbidity,
  });
}

if (cohort?.countries) {
  items.push({
    label: "Countries",
    type: "ONTOLOGY",
    content: cohort.countries,
  });
}

if (cohort?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: cohort.inclusionCriteria,
  });
}

// todo add count table ( empty in current test set)
</script>

<template>
  <ContentBlockModal
    v-if="cohort"
    :title="cohort?.name"
    :description="cohort?.description"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
