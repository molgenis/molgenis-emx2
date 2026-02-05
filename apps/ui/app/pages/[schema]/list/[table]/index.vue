<script setup lang="ts">
import { encodeRecordId } from "../../../../../../tailwind-components/app/utils/recordIdEncoder";
import { extractPrimaryKey } from "../../../../../../tailwind-components/app/utils/extractPrimaryKey";
import fetchTableMetadata from "../../../../../../tailwind-components/app/composables/fetchTableMetadata";
import Emx2ListView from "../../../../../../tailwind-components/app/components/display/Emx2ListView.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type {
  IColumn,
  IRow,
  IRefColumn,
  ITableMetaData,
} from "../../../../../../metadata-utils/src/types";
import { useRoute } from "#app/composables/router";
import { useHead } from "#app";

const route = useRoute();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

useHead({ title: `${tableId} - ${schemaId} - Molgenis` });

const tableMetadata: ITableMetaData = await fetchTableMetadata(
  schemaId,
  tableId
);

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata?.label || tableId, url: "" },
];

function getHref(col: IColumn, row: IRow): string {
  const refCol = col as IRefColumn;
  const targetSchema = refCol.refSchemaId || schemaId;
  const targetTable = refCol.refTableId || tableId;
  const pk = extractPrimaryKey(row, tableMetadata);
  return `/${targetSchema}/view/${targetTable}?${encodeRecordId(pk)}`;
}
</script>

<template>
  <section class="mx-auto lg:px-[30px] px-0">
    <PageHeader :title="tableMetadata?.label || tableId" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>

    <Emx2ListView
      :schema-id="schemaId"
      :table-id="tableId"
      :show-search="true"
      :paging-limit="10"
      :get-href="getHref"
    />
  </section>
</template>
