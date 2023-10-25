<script setup lang="ts">
import { KeyObject } from "~/interfaces/types";
import variableQuery from "~~/gql/variable";

const query = moduleToString(variableQuery);

const props = defineProps<{
  variableKey: KeyObject;
}>();

const config = useRuntimeConfig();
const route = useRoute();

const { data, pending, error } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: query,
      variables: { filter: buildFilterFromKeysObject(props.variableKey) },
    },
  }
).catch((e) => console.log(e));

const items = computed(() => [
  {
    label: "Unit",
    content: data.value.data?.Variables[0]?.unit?.name || "-",
  },
  {
    label: "Formats",
    content: data.value.data?.Variables[0]?.format?.name || "-",
  },
  {
    label: "n repeats",
    content: data.value.data?.RepeatedVariables_agg.count || "None",
  },
]);
</script>

<template>
  <ContentBlockModal
    :title="data.data?.Variables[0].name"
    :description="data.data?.Variables[0].description"
    sub-title="Variable"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
