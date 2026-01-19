<script setup lang="ts">
import {
  decodeRecordId,
  encodeRecordId,
} from "../../../../../../../tailwind-components/app/utils/recordIdEncoder";
import fetchTableMetadata from "../../../../../../../tailwind-components/app/composables/fetchTableMetadata";
import DetailPageLayout from "../../../../../../../tailwind-components/app/components/layout/DetailPageLayout.vue";
import SideNav from "../../../../../../../tailwind-components/app/components/SideNav.vue";
import Emx2RecordView from "../../../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";
import BreadCrumbs from "../../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../../../tailwind-components/app/components/PageHeader.vue";
import type { Crumb } from "../../../../../../../tailwind-components/types/types";
import type {
  IColumn,
  IRow,
  IRefColumn,
  ITableMetaData,
} from "../../../../../../../metadata-utils/src/types";
import { useRoute, useRouter } from "#app/composables/router";
import { useHead } from "#app";
import { computed } from "vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const recordIdEncoded = route.params.recordId as string;

// Decode the record ID
const rowId = computed(() => {
  try {
    return decodeRecordId(recordIdEncoded);
  } catch (e) {
    return {};
  }
});

// Page title
useHead({ title: `${tableId} - ${schemaId} - Molgenis` });

// Fetch metadata
const tableMetadata: ITableMetaData = await fetchTableMetadata(
  schemaId,
  tableId
);

// Compute sections for SideNav from metadata SECTION and HEADING columns
const sections = computed(() => {
  if (!tableMetadata?.columns) return [];

  const result: Array<{ id: string; label: string }> = [];

  const sectionColumns = tableMetadata.columns.filter(
    (c: IColumn) => c.columnType === "SECTION"
  );
  const headingColumns = tableMetadata.columns.filter(
    (c: IColumn) => c.columnType === "HEADING"
  );

  // SECTIONs with their nested HEADINGs
  for (const section of sectionColumns) {
    result.push({ id: section.id, label: section.label || section.id });
    const sectionHeadings = headingColumns.filter(
      (h: IColumn) => h.section === section.id
    );
    for (const heading of sectionHeadings) {
      result.push({ id: heading.id, label: heading.label || heading.id });
    }
  }

  // Orphan HEADINGs (not in any section)
  const orphanHeadings = headingColumns.filter((h: IColumn) => !h.section);
  for (const heading of orphanHeadings) {
    result.push({ id: heading.id, label: heading.label || heading.id });
  }

  return result;
});

// Breadcrumbs
const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata?.label || tableId, url: `/${schemaId}/${tableId}` },
  { label: "Record", url: "" },
];

// Extract primary key from referenced row data
function extractPrimaryKey(col: IColumn, row: IRow): Record<string, any> {
  const refCol = col as IRefColumn;
  // For refs, the row IS the referenced data (pk fields included)
  // Extract key columns from the ref table
  return row;
}

// Navigate to another record view when ref is clicked
function getRefClickAction(col: IColumn, row: IRow) {
  return () => {
    const refCol = col as IRefColumn;
    const targetSchema = refCol.refSchemaId || schemaId;
    const targetTable = refCol.refTableId;
    if (!targetTable) return;

    const pk = extractPrimaryKey(col, row);
    const encodedPk = encodeRecordId(pk);
    router.push(`/${targetSchema}/view/${targetTable}/${encodedPk}`);
  };
}
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

    <template #side>
      <SideNav
        v-if="sections.length"
        :title="(tableMetadata?.label || tableId).toUpperCase()"
        :sections="sections"
        :scroll-offset="80"
      />
    </template>

    <template #main>
      <Emx2RecordView
        :key="`${schemaId}-${tableId}-${recordIdEncoded}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :row-id="rowId"
        :get-ref-click-action="getRefClickAction"
      />
    </template>
  </DetailPageLayout>
</template>
