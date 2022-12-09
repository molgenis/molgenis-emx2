<script setup lang="ts">
import { Ref } from "vue";
import query from "~~/gql/subcohort";
const config = useRuntimeConfig();
const route = useRoute();

const { id } = defineProps<{
  id: string;
}>();

if (query.loc?.source.body === undefined) {
  throw "unable to load query: " + query.toString();
}
const queryValue = query.loc?.source.body;

let subcohort: Ref = ref();
const { data: subcohortData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: queryValue,
      variables: { pid: route.params.cohort, name: id },
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
if (subcohort?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(subcohort.ageGroups, toName),
  });
}
if (subcohort?.inclusionStart || subcohort?.inclusionEnd) {
  items.push({
    label: "Start/end year: ",
    content: filters.startEndYear(
      subcohort.inclusionStart,
      subcohort.inclusionEnd
    ),
  });
}
if (subcohort?.countries) {
  items.push({
    label: "Population",
    content: renderList(subcohort.countries, toName),
  });
}
if (subcohort?.mainMedicalCondition) {
  items.push({
    label: "Main medical condition",
    content: renderList(subcohort.mainMedicalCondition, toName, toCommaList),
  });
}

if (subcohort?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subcohort.inclusionCriteria,
  });
}
if (subcohort?.comorbidity) {
  items.push({
    label: "Comorbidity",
    content: renderList(subcohort.comorbidity, toName),
  });
}

items.sort((a, b) => a.label.localeCompare(b.label));

// todo add count table ( empty in current test set)
</script>

<template>
  <ContentBlock
    :title="subcohort?.name"
    :description="subcohort?.description"
    v-if="subcohort"
  >
    <DefinitionList :items="items" :small="true" />
  </ContentBlock>
</template>
