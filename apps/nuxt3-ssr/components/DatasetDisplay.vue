<script setup lang="ts">
import datasetGql from "~~/gql/datasetDetails";
const route = useRoute();

const { id } = defineProps<{
  id: string;
}>();

const query = moduleToString(datasetGql);

const { data } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: {
    query: query,
    variables: { id: route.params.datasource, name: id },
  },
});

const dataset = computed(() => {
  return data.value.data.Datasets[0];
});
</script>

<template>
  <ContentBlockModal
    v-if="dataset"
    :title="dataset.name"
    :description="dataset.description"
  >
    <CatalogueItemList :items="[]" :small="true" />
  </ContentBlockModal>
</template>
