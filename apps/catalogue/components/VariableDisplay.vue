<script setup lang="ts">
import variableQuery from "~~/gql/variable";
import type { KeyObject } from "metadata-utils";
import { buildFilterFromKeysObject } from "metadata-utils";

const query = moduleToString(variableQuery);

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
      variables: {
        variableFilter: buildFilterFromKeysObject(props.variableKey),
      },
    },
  }
).catch((e) => console.log(e));

const variable = computed(() => data.value.data?.Variables[0]);

const items = computed(() => [
  {
    label: "Unit",
    content: variable.value?.unit?.name || "-",
  },
  {
    label: "Formats",
    content: variable.value?.format?.name || "-",
  },
  {
    label: "Repeated for",
    content:
      variable.value?.repeatUnit?.name ||
      variable.value?.repeatMin ||
      variable.value?.repeatMax
        ? variable.value?.repeatUnit?.name +
          " " +
          variable.value?.repeatMin +
          "-" +
          variable.value?.repeatMax
        : undefined,
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
