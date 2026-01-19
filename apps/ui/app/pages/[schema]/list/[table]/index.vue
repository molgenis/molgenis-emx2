<script setup lang="ts">
import { encodeRecordId } from "../../../../../../tailwind-components/app/utils/recordIdEncoder";
import fetchTableMetadata from "../../../../../../tailwind-components/app/composables/fetchTableMetadata";
import Emx2ListView from "../../../../../../tailwind-components/app/components/display/Emx2ListView.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import type { Crumb } from "../../../../../../tailwind-components/types/types";
import type {
  IColumn,
  IRow,
  IRefColumn,
} from "../../../../../../metadata-utils/src/types";
import { useRoute, useRouter } from "#app/composables/router";
import { useHead } from "#app";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

useHead({ title: `${tableId} - ${schemaId} - Molgenis` });

const tableMetadata = await fetchTableMetadata(schemaId, tableId);

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata?.label || tableId, url: "" },
];

function getRefClickAction(col: IColumn, row: IRow) {
  return () => {
    const refCol = col as IRefColumn;
    const targetSchema = refCol.refSchemaId || schemaId;
    const targetTable = refCol.refTableId || tableId;

    // row contains the full record data, use it as pk
    const pk = row;
    const encodedPk = encodeRecordId(pk);
    router.push(`/${targetSchema}/view/${targetTable}/${encodedPk}`);
  };
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
      :get-ref-click-action="getRefClickAction"
    />
  </section>
</template>
