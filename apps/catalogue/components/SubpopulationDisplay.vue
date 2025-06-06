<script setup lang="ts">
import subpopulationGql from "~~/gql/subpopulation";
import dateUtils from "~/utils/dateUtils";
import type { ISubpopulations } from "~/interfaces/catalogue";
import type { Resp } from "../../tailwind-components/types/types";
import { useRoute, useFetch, showError, useRuntimeConfig } from "#app";
import { moduleToString, removeChildIfParentSelected } from "#imports";
import { computed } from "vue";
const route = useRoute();
const config = useRuntimeConfig();
const schema = config.public.schema as string;

const { id } = defineProps<{
  id: string;
}>();

const query = moduleToString(subpopulationGql);

const { data, error } = await useFetch<Resp<ISubpopulations>>(
  `/${schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { id: route.params.resource, name: id },
    },
  }
);

if (error.value) {
  showError(error.value);
}

const subpopulation = computed(
  () => data.value?.data?.Subpopulations[0] as ISubpopulations
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

const items: any = [];

if (subpopulation.value.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subpopulation.value.numberOfParticipants,
  });
}

if (subpopulation.value.inclusionStart || subpopulation.value.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(
      subpopulation.value.inclusionStart,
      subpopulation.value.inclusionEnd
    ),
  });
}

if (subpopulation.value.ageGroups?.length) {
  items.push({
    label: "Age categories",
    content: renderList(
      removeChildIfParentSelected(subpopulation.value.ageGroups),
      toName
    ),
  });
}

if (subpopulation.value.mainMedicalCondition) {
  items.push({
    label: "Main medical condition",
    type: "ONTOLOGY",
    content: subpopulation.value.mainMedicalCondition,
  });
}

if (subpopulation.value.comorbidity) {
  items.push({
    label: "Comorbidity",
    type: "ONTOLOGY",
    content: subpopulation.value.comorbidity,
  });
}

if (subpopulation.value.countries) {
  items.push({
    label: "Countries",
    type: "ONTOLOGY",
    content: subpopulation.value.countries,
  });
}

if (subpopulation.value.inclusionCriteria) {
  items.push({
    label: "Inclusion criteria",
    content: renderList(
      subpopulation.value.inclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation.value.otherInclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subpopulation.value.otherInclusionCriteria,
  });
}

if (subpopulation.value.exclusionCriteria) {
  items.push({
    label: "Exclusion criteria",
    content: renderList(
      subpopulation.value.exclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation.value.otherExclusionCriteria) {
  items.push({
    label: "Other exclusion criteria",
    content: subpopulation.value.otherExclusionCriteria,
  });
}
</script>

<template>
  <ContentBlockModal
    :title="subpopulation.name"
    :description="subpopulation.description"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
