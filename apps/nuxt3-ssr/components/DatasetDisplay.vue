<script setup lang="ts">
import type { IDefinitionListItem } from "~/interfaces/types";
import datasetGql from "~~/gql/datasetDetails";
const route = useRoute();

const { name, resourceId } = defineProps<{
  name: string;
  resourceId: string;
}>();

const query = moduleToString(datasetGql);

const { data } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: {
    query: query,
    variables: { id: resourceId, name },
  },
});

const dataset = computed(() => {
  return data.value.data.Datasets[0];
});

const items: IDefinitionListItem[] = [];

if (dataset.value.label) {
  items.push({
    label: "Label",
    content: dataset.value.label,
  });
}

if (dataset.value.unitOfObservation) {
  items.push({
    label: "Unit of observation",
    content: dataset.value.numberOfParticipants,
  });
}

if (dataset.value.keywords) {
  items.push({
    label: "Keywords",
    content: dataset.value.keywords,
    type: "ONTOLOGY",
  });
}

if (dataset.value.numberOfRows) {
  items.push({
    label: "NumberOfRows",
    content: dataset.value.numberOfRows,
  });
}

if (dataset.value.mappedTo) {
  items.push({
    label: "Mapped to",
    content: dataset.value.mappedTo,
    type: "MAPPED",
  });
}

if (dataset.value.mappedFrom) {
  items.push({
    label: "Mapped from",
    content: dataset.value.mappedFrom,
    type: "MAPPED",
  });
}

if (dataset.value.sinceVersion) {
  items.push({
    label: "Since version",
    content: dataset.value.sinceVersion,
  });
}

if (dataset.value.untilVersion) {
  items.push({
    label: "Until version",
    content: dataset.value.untilVersion,
  });
}
</script>

<template>
  <ContentBlockModal
    v-if="dataset"
    :title="dataset.name"
    :description="dataset.description"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
