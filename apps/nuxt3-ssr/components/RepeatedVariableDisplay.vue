<script setup lang="ts">
import repeatedVariableQuery from "~~/gql/repeatedVariable";
import type { KeyObject } from "metadata-utils";
import { buildFilterFromKeysObject } from "metadata-utils";

const query = moduleToString(repeatedVariableQuery);

const props = defineProps<{
  variableKey: KeyObject;
}>();

const route = useRoute();

const { data, pending, error } = await useFetch(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { filter: buildFilterFromKeysObject(props.variableKey) },
    },
  }
).catch((e) => console.log(e));

const repeatedVariable = data.value.data?.RepeatedVariables[0];

const items = computed(() => [
  {
    label: "Repeat of",
    content: repeatedVariable?.isRepeatOf.format?.name || "-",
  },
  {
    label: "Unit",
    content: repeatedVariable?.isRepeatOf.unit?.name || "-",
  },
  {
    label: "Formats",
    content: repeatedVariable?.isRepeatOf.format?.name || "-",
  },
]);
</script>

<template>
  <ContentBlockModal
    :title="repeatedVariable.name"
    :description="repeatedVariable.label"
    sub-title="Repeated Variable"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
