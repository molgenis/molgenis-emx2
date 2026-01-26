<script setup lang="ts">
import { useRoute, useRuntimeConfig, useHead } from "#app";
import { computed } from "vue";
import type { Crumb } from "../../../../../../../tailwind-components/types/types";
import type {
  IDisplayConfig,
  IColumn,
  IRow,
  IRefColumn,
} from "../../../../../../../metadata-utils/src/types";
import LayoutsDetailPage from "../../../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Emx2RecordView from "../../../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";

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

// Filter for variables belonging to this dataset
const variablesFilter = {
  dataset: {
    resource: { id: { equals: resourceId } },
    name: { equals: datasetName },
  },
};

const displayConfig = computed(() => {
  const columnConfig = new Map<string, IDisplayConfig>();

  columnConfig.set("resource", {
    getHref: (_col: IColumn, row: IRow) => {
      const id = row?.id || row?.name;
      return `/${catalogue}/collections/${id}`;
    },
  });

  columnConfig.set("variables", {
    component: "table",
    visibleColumns: ["name", "label", "format", "unit"],
    pageSize: 20,
    filter: variablesFilter,
    getHref: (_col: IColumn, row: IRow) => {
      const varName = row?.name;
      return `/${catalogue}/variables/${varName}`;
    },
  });

  return columnConfig;
});

// Virtual REFBACK column for variables
const extraColumns = computed<IColumn[]>(() => [
  {
    id: "variables",
    label: "Variables",
    columnType: "REFBACK",
    refTableId: "Variables",
    refSchemaId: schema,
    refLabel: "${name}",
    refLabelDefault: "${name}",
  } as IRefColumn,
]);

const crumbs: Crumb[] = [
  { label: catalogue, url: `/${catalogue}` },
  { label: "datasets", url: `/${catalogue}/datasets` },
  {
    label: resourceId,
    url: `/${catalogue}/datasets?conditions=[{"id":"search","search":"${resourceId}"}]`,
  },
  { label: datasetName, url: "" },
];
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="datasetName"
        :description="`Dataset from ${resourceId}`"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <Emx2RecordView
        :schema-id="schema"
        table-id="Datasets"
        :row-id="rowId"
        :display-config="displayConfig"
        :extra-columns="extraColumns"
      />
    </template>
  </LayoutsDetailPage>
</template>
