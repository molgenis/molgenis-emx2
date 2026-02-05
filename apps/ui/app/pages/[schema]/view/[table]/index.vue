<script setup lang="ts">
import {
  decodeRecordId,
  encodeRecordId,
} from "../../../../../../tailwind-components/app/utils/recordIdEncoder";
import fetchTableMetadata from "../../../../../../tailwind-components/app/composables/fetchTableMetadata";
import DetailPageLayout from "../../../../../../tailwind-components/app/components/layout/DetailPageLayout.vue";
import SideNav from "../../../../../../tailwind-components/app/components/SideNav.vue";
import Emx2RecordView from "../../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type {
  IColumn,
  IRow,
  ITableMetaData,
  IDisplayConfig,
} from "../../../../../../metadata-utils/src/types";
import { useRoute } from "#app/composables/router";
import { useHead } from "#app";
import { computed } from "vue";

const route = useRoute();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

const rowId = computed(() => {
  const queryString = new URLSearchParams(
    route.query as Record<string, string>
  ).toString();
  if (!queryString) return {};
  try {
    return decodeRecordId(queryString);
  } catch (e) {
    return {};
  }
});

useHead({ title: `${tableId} - ${schemaId} - Molgenis` });

const tableMetadata: ITableMetaData = await fetchTableMetadata(
  schemaId,
  tableId
);

const sections = computed(() => {
  if (!tableMetadata?.columns) return [];

  const result: Array<{ id: string; label: string }> = [];

  const sectionColumns = tableMetadata.columns.filter(
    (c: IColumn) => c.columnType === "SECTION"
  );
  const headingColumns = tableMetadata.columns.filter(
    (c: IColumn) => c.columnType === "HEADING"
  );

  for (const section of sectionColumns) {
    result.push({ id: section.id, label: section.label || section.id });
    const sectionHeadings = headingColumns.filter(
      (h: IColumn) => h.section === section.id
    );
    for (const heading of sectionHeadings) {
      result.push({ id: heading.id, label: heading.label || heading.id });
    }
  }

  const orphanHeadings = headingColumns.filter((h: IColumn) => !h.section);
  for (const heading of orphanHeadings) {
    result.push({ id: heading.id, label: heading.label || heading.id });
  }

  return result;
});

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata?.label || tableId, url: `/${schemaId}/${tableId}` },
  { label: "Record", url: "" },
];

const REF_TYPES = [
  "REF",
  "REF_ARRAY",
  "REFBACK",
  "RADIO",
  "SELECT",
  "MULTISELECT",
  "CHECKBOX",
];

const displayConfig = computed(() => {
  const config = new Map<string, IDisplayConfig>();
  if (!tableMetadata?.columns) return config;

  for (const col of tableMetadata.columns) {
    if (REF_TYPES.includes(col.columnType)) {
      config.set(col.id, {
        getHref: (column: IColumn, row: IRow) => {
          const targetSchema = column.refSchemaId || schemaId;
          const targetTable = column.refTableId;
          if (!targetTable) return "";
          return `/${targetSchema}/view/${targetTable}?${encodeRecordId(row)}`;
        },
      });
    }
  }
  return config;
});

const rowIdKey = computed(() => JSON.stringify(rowId.value));
</script>

<template>
  <DetailPageLayout :show-side-nav="sections.length > 0">
    <template #header>
      <PageHeader :title="tableMetadata?.label || tableId" align="left">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" align="left" />
        </template>
      </PageHeader>
    </template>

    <template #sidebar>
      <SideNav
        v-if="sections.length"
        :title="(tableMetadata?.label || tableId).toUpperCase()"
        :sections="sections"
        :scroll-offset="80"
      />
    </template>

    <template #main>
      <Emx2RecordView
        :key="rowIdKey"
        :schema-id="schemaId"
        :table-id="tableId"
        :row-id="rowId"
        :display-config="displayConfig"
      />
    </template>
  </DetailPageLayout>
</template>
