<script setup lang="ts">
import type { Crumb } from "../../../../../tailwind-components/types/types";
import type {
  IDisplayConfig,
  IColumn,
  IRow,
} from "../../../../../metadata-utils/src/types";
import { useRoute, useHead, useRuntimeConfig, navigateTo } from "#app";
import { computed } from "vue";
import LayoutsLandingPage from "../../../components/layouts/LandingPage.vue";
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

const displayConfig: IDisplayConfig = {
  layout: "table",
  showLayoutToggle: true,
  showFilters: true,
  pageSize: 20,
  visibleColumns: ["name", "label", "resource", "description", "datasetType"],
  columnConfig: {
    name: {
      clickAction: (_col: IColumn, row: IRow) => {
        const resourceId = row.resource?.id;
        const name = row.name;
        const url = `/${catalogueRouteParam}/datasets/${resourceId}/${name}`;
        navigateTo(url);
      },
    },
  },
};

const crumbs: Crumb[] = [
  { label: catalogueRouteParam, url: `/${catalogueRouteParam}` },
  { label: "datasets", url: "" },
];
</script>

<template>
  <LayoutsLandingPage>
    <Emx2DataView
      :schema-id="schema"
      table-id="Datasets"
      :config="displayConfig"
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
  </LayoutsLandingPage>
</template>
