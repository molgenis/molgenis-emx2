<script setup lang="ts">
import variableQuery from "../gql/variable";
import type { KeyObject } from "../../metadata-utils/src/types";
import type { IVariables } from "../interfaces/catalogue";
import type { Resp } from "../../tailwind-components/types/types";
import { useRoute, useFetch, showError } from "#app";
import { moduleToString } from "#imports";
import { computed } from "vue";
import { buildFilterFromKeysObject } from "../../metadata-utils/src";

const query = moduleToString(variableQuery);

const props = defineProps<{
  variableKey: KeyObject;
}>();

const route = useRoute();

const { data, error } = await useFetch<Resp<IVariables>>(
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
);

if (error.value) {
  showError(error.value);
}

const variable = computed(() => data.value?.data?.Variables[0] as IVariables);

const items = computed(() => {
  const defaultItems = [
    {
      label: "Label",
      content: variable.value.label || "-",
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
    {
      label: "Unit",
      content: variable.value?.unit?.name || "-",
    },
  ];

  if (variable.value.dataset) {
    defaultItems.push({
      label: "Dataset",
      content:
        variable.value.dataset.resource.id +
        " - " +
        variable.value.dataset.name,
    });
  }

  return defaultItems;
});
</script>

<template>
  <ContentBlockModal
    :title="variable.name"
    :description="variable.description"
    sub-title="Variable"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
