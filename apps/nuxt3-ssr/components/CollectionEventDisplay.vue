<script setup lang="ts">
import { Ref } from "vue";
import query from "~~/gql/collectionEvent";

const { id: collectionEventName } = defineProps<{
  id: string;
}>();

const config = useRuntimeConfig();
const route = useRoute();

if (query.loc?.source.body === undefined) {
  throw "unable to load query: " + query.toString();
}
const queryValue = query.loc?.source.body;

let collectionEvent: Ref<ICollectionEvent | undefined> = ref();

const { data: collectionEventData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: queryValue,
      variables: {
        id: route.params.cohort,
        name: collectionEventName,
      },
    },
  }
).catch((e) => console.log(e));

watch(
  collectionEventData,
  (data) => {
    collectionEvent = data?.data?.CollectionEvents[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

const pageCrumbs: any = {
  Cohorts: `/${route.params.schema}/ssr-catalogue`,
};

// @ts-ignore
pageCrumbs[
  route.params.cohort
] = `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}`;

function renderList(list: any[], itemMapper: (a: any) => string) {
  return list?.length === 1 ? itemMapper(list[0]) : list.map(itemMapper);
}

const toName = (item: any) => item.name;

const items = [];

if (collectionEvent?.subcohorts?.length) {
  items.push({
    label: "Subcohorts",
    content: renderList(collectionEvent?.subcohorts, toName),
  });
}

if (collectionEvent?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collectionEvent?.numberOfParticipants,
  });
}

if (collectionEvent?.startYear || collectionEvent?.endYear) {
  items.push({
    label: "Start/end year",
    content: filters.startEndYear(
      collectionEvent.startYear && collectionEvent.startYear.name
        ? collectionEvent.startYear.name
        : null,
      collectionEvent.endYear && collectionEvent.endYear.name
        ? collectionEvent.endYear.name
        : null
    ),
  });
}

if (collectionEvent?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(
      removeChildIfParentSelected(collectionEvent.ageGroups),
      toName
    ),
  });
}

if (collectionEvent?.areasOfInformation?.length) {
  items.push({
    label: "Areas of information",
    type: "ONTOLOGY",
    content: buildOntologyTree(collectionEvent.areasOfInformation),
  });
}

if (collectionEvent?.dataCategories?.length) {
  items.push({
    label: "Data Categories",
    type: "ONTOLOGY",
    content: buildOntologyTree(collectionEvent.dataCategories),
  });
}

if (collectionEvent?.sampleCategories?.length) {
  items.push({
    label: "Sample categories",
    type: "ONTOLOGY",
    content: buildOntologyTree(collectionEvent.sampleCategories),
  });
}

if (collectionEvent?.coreVariables?.length) {
  items.push({
    label: "Core variables",
    content: collectionEvent?.coreVariables,
  });
}
</script>

<template>
  <ContentBlock
    :title="collectionEvent?.name"
    :description="collectionEvent?.description"
    v-if="collectionEvent"
  >
    <DefinitionList :items="items" :small="true" />
  </ContentBlock>
</template>
