<script setup lang="ts">
import collectionEventGql from "~~/gql/collectionEvent";
import ContentBlockModal from "./content/ContentBlockModal.vue";
import type { IDefinitionListItem, IMgError } from "~~/interfaces/types";

const { id: collectionEventName } = defineProps<{
  id: string;
}>();

const config = useRuntimeConfig();
const route = useRoute();
const query = moduleToString(collectionEventGql);

const { data, error } = await useFetch<any, IMgError>(
  `/${route.params.schema}/graphql`,
  {
    key: `collection-event-${route.params.cohort}-${collectionEventName}`,
    method: "POST",
    body: {
      query,
      variables: {
        id: route.params.cohort,
        name: collectionEventName,
      },
    },
  }
);

if (error.value) {
  const contextMsg = "Error fetching data for collection-event preview fetch";
  logError(error.value, contextMsg);
  throw new Error(contextMsg);
}

const collectionEvent: any = computed(
  () => data.value.data.CollectionEvents[0]
);

const pageCrumbs: any = {
  Cohorts: `/${route.params.schema}/ssr-catalogue`,
};

pageCrumbs[
  route.params.cohort as string
] = `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}`;

function renderList(list: any[], itemMapper: (a: any) => string) {
  return list?.length === 1 ? itemMapper(list[0]) : list.map(itemMapper);
}

const toName = (item: any) => item.name;

const items: IDefinitionListItem[] = [];

if (collectionEvent.value?.subcohorts?.length) {
  items.push({
    label: "Subcohorts",
    content: renderList(collectionEvent.value?.subcohorts, toName),
  });
}

if (collectionEvent.value?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collectionEvent.value?.numberOfParticipants,
  });
}

if (collectionEvent.value?.startYear || collectionEvent.value?.endYear) {
  items.push({
    label: "Start/end year",
    content: filters.startEndYear(
      collectionEvent.value.startYear && collectionEvent.value.startYear.name
        ? collectionEvent.value.startYear.name
        : null,
      collectionEvent.value.endYear && collectionEvent.value.endYear.name
        ? collectionEvent.value.endYear.name
        : null
    ),
  });
}

if (collectionEvent.value?.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(
      removeChildIfParentSelected(collectionEvent.value.ageGroups),
      toName
    ),
  });
}

if (collectionEvent.value?.areasOfInformation?.length) {
  items.push({
    label: "Areas of information",
    type: "ONTOLOGY",
    content: collectionEvent.value.areasOfInformation,
  });
}

if (collectionEvent.value?.dataCategories?.length) {
  items.push({
    label: "Data Categories",
    type: "ONTOLOGY",
    content: collectionEvent.value.dataCategories,
  });
}

if (collectionEvent.value?.sampleCategories?.length) {
  items.push({
    label: "Sample categories",
    type: "ONTOLOGY",
    content: collectionEvent.value.sampleCategories,
  });
}
</script>

<template>
  <ContentBlockModal
    v-if="collectionEvent"
    :title="collectionEvent?.name"
    :description="collectionEvent?.description"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
