<script setup lang="ts">
import subpopulationGql from "~~/gql/subpopulation";
import dateUtils from "~/utils/dateUtils";
const route = useRoute();

const { id } = defineProps<{
  id: string;
}>();

const query = moduleToString(subpopulationGql);

let subpopulation: Ref = ref();
const { data: subpopulationData } = await useFetch(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { id: route.params.resource, name: id },
    },
  }
).catch((e) => console.log(e));

watch(
  subpopulationData,
  function setData(data: any) {
    subpopulation = data?.data?.Subpopulations[0];
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

if (subpopulation?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subpopulation.numberOfParticipants,
  });
}

if (subpopulation?.inclusionStart || subpopulation?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(
      subpopulation.inclusionStart,
      subpopulation.inclusionEnd
    ),
  });
}

if (subpopulation?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(
      removeChildIfParentSelected(subpopulation.ageGroups),
      toName
    ),
  });
}

if (subpopulation?.mainMedicalCondition) {
  items.push({
    label: "Main medical condition",
    type: "ONTOLOGY",
    content: subpopulation.mainMedicalCondition,
  });
}

if (subpopulation?.comorbidity) {
  items.push({
    label: "Comorbidity",
    type: "ONTOLOGY",
    content: subpopulation.comorbidity,
  });
}

if (subpopulation?.countries) {
  items.push({
    label: "Countries",
    type: "ONTOLOGY",
    content: subpopulation.countries,
  });
}

if (subpopulation?.inclusionCriteria) {
  items.push({
    label: "Inclusion criteria",
    content: renderList(
      subpopulation.inclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation?.otherInclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subpopulation.otherInclusionCriteria,
  });
}

if (subpopulation?.exclusionCriteria) {
  items.push({
    label: "Exclusion criteria",
    content: renderList(
      subpopulation.exclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation?.otherExclusionCriteria) {
  items.push({
    label: "Other exclusion criteria",
    content: subpopulation.otherExclusionCriteria,
  });
}

// todo add count table ( empty in current test set)
</script>

<template>
  <ContentBlockModal
    v-if="subpopulation"
    :title="subpopulation?.name"
    :description="subpopulation?.description"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
