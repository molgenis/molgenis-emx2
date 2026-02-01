<script setup lang="ts">
import type { Crumb } from "../../../../../tailwind-components/types/types";
import { useRoute, useHead, useRuntimeConfig } from "#app";
import { computed } from "vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import Emx2DataView from "../../../../../tailwind-components/app/components/display/Emx2DataView.vue";

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const route = useRoute();
const catalogueRouteParam = route.params.catalogue as string;
const scoped = catalogueRouteParam !== "all";

const titlePrefix = scoped ? catalogueRouteParam + " " : "";
useHead({
  title: titlePrefix + "Datasets",
  meta: [
    {
      name: "description",
      content: `Overview of ${titlePrefix.trim()} datasets`,
    },
  ],
});

const catalogueFilter = computed(() => {
  if (!scoped) return undefined;
  return {
    resource: {
      _or: [
        { id: { equals: catalogueRouteParam } },
        { partOfNetworks: { id: { equals: catalogueRouteParam } } },
        {
          partOfNetworks: {
            parentNetworks: { id: { equals: catalogueRouteParam } },
          },
        },
      ],
    },
  };
});

const crumbs: Crumb[] = [
  { label: catalogueRouteParam, url: `/${catalogueRouteParam}` },
  { label: "datasets", url: "" },
];
</script>

<template>
  <Emx2DataView
    :schema-id="schema"
    table-id="Datasets"
    :config="{
      layout: 'cards',
      showFilters: true,
      showLayoutToggle: true,
      pageSize: 20,
      visibleColumns: ['name', 'label', 'resource', 'description', 'datasetType']
    }"
    :static-filter="catalogueFilter"
  >
    <template #header>
      <PageHeader
        title="Datasets"
        description="Overview of datasets"
        icon="image-table"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" current="datasets" />
        </template>
      </PageHeader>
    </template>
  </Emx2DataView>
</template>
