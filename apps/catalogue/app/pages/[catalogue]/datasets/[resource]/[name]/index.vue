<script setup lang="ts">
import { useRoute, useRuntimeConfig, useHead } from "#app";
import { computed } from "vue";
import type { Crumb } from "../../../../../../../tailwind-components/types/types";
import type {
  IDisplayConfig,
  IColumn,
  IRow,
} from "../../../../../../../metadata-utils/src/types";
import LayoutsLandingPage from "../../../../../components/layouts/LandingPage.vue";
import PageHeader from "../../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Emx2RecordView from "../../../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";
import Emx2DataView from "../../../../../../../tailwind-components/app/components/display/Emx2DataView.vue";

const config = useRuntimeConfig();
const route = useRoute();

const schema = config.public.schema as string;
const catalogue = route.params.catalogue as string;
const resourceId = route.params.resource as string;
const datasetName = route.params.name as string;

useHead({
  title: `${datasetName} - Dataset`,
  meta: [
    {
      name: "description",
      content: `Dataset ${datasetName} from ${resourceId}`,
    },
  ],
});

const rowId = computed(() => ({
  resource: { id: resourceId },
  name: datasetName,
}));

const variablesFilter = computed(() => ({
  dataset: {
    resource: { id: { equals: resourceId } },
    name: { equals: datasetName },
  },
}));

const recordDisplayConfig: IDisplayConfig = {
  columnConfig: {
    mg_top_of_form: { label: "Dataset" },
    resource: {
      getHref: (_col: IColumn, row: IRow) => {
        const id = row?.id || row?.name;
        return `/${catalogue}/collections/${id}`;
      },
    },
  },
};

const variablesDisplayConfig: IDisplayConfig = {
  layout: "table",
  showFilters: true,
  showLayoutToggle: true,
  pageSize: 20,
  visibleColumns: ["name", "label", "format", "unit"],
  columnConfig: {
    name: {
      getHref: (_col: IColumn, row: IRow) => {
        const varName = row?.name;
        return `/${catalogue}/variables/${varName}`;
      },
    },
  },
};

const crumbs: Crumb[] = [
  { label: catalogue, url: `/${catalogue}` },
  { label: "datasets", url: `/${catalogue}/datasets` },
  { label: resourceId, url: `/${catalogue}/datasets/${resourceId}` },
  { label: datasetName, url: "" },
];
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      :title="datasetName"
      :description="`Dataset from ${resourceId}`"
    >
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" />
      </template>
    </PageHeader>

    <Emx2RecordView
      :schema-id="schema"
      table-id="Datasets"
      :row-id="rowId"
      :config="recordDisplayConfig"
    />

    <div class="mt-8">
      <h2 class="text-xl font-semibold mb-3 text-record-heading">
        Variables
      </h2>
      <Emx2DataView
        :schema-id="schema"
        table-id="Variables"
        :config="variablesDisplayConfig"
        :static-filter="variablesFilter"
        :url-sync="false"
      />
    </div>
  </LayoutsLandingPage>
</template>
