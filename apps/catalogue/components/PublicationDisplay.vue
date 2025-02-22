<script setup lang="ts">
import publicationQuery from "~~/gql/publication";
import type { IPublication } from "~/interfaces/types";

const query = moduleToString(publicationQuery);

const props = defineProps<{
  doi: string;
}>();

const route = useRoute();

const { data } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: {
    query: query,
    variables: { filter: { doi: props.doi } },
  },
}).catch((e) => console.log(e));

const publication: IPublication = data.value.data?.Publications[0];

const items = computed(() => [
  {
    label: "DOI",
    content: publication.doi || "-",
  },
  {
    label: "Authors",
    content: publication?.authors?.join(", ") || "-",
  },
  {
    label: "Year",
    content: publication?.year || "-",
  },
  {
    label: "Journal",
    content: publication?.journal || "-",
  },
  {
    label: "Volume",
    content: publication?.volume || "-",
  },
  {
    label: "Number",
    content: publication?.number || "-",
  },
  {
    label: "Pagination",
    content: publication?.pagination || "-",
  },
  {
    label: "Publisher",
    content: publication?.publisher || "-",
  },
  {
    label: "School",
    content: publication?.school || "-",
  },
  {
    label: "Abstract",
    content: publication?.abstract || "-",
  },
]);
</script>

<template>
  <ContentBlockModal
    :title="publication.title ?? publication.doi"
    sub-title="Publication"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
