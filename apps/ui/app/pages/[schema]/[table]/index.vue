<script setup lang="ts">
import { computed } from "vue";
import type { Crumb } from "../../../../../tailwind-components/types/types";
import type {
  IColumn,
  IRow,
  IDisplayConfig,
} from "../../../../../metadata-utils/src/types";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useRoute } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import { useHead } from "#app";
import Emx2DataView from "../../../../../tailwind-components/app/components/display/Emx2DataView.vue";
import DetailPageLayout from "../../../../../tailwind-components/app/components/layout/DetailPageLayout.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import { encodeRecordId } from "../../../../../tailwind-components/app/utils/recordIdEncoder";

const route = useRoute();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

useHead({ title: `${tableId} - ${schemaId}  - Molgenis` });

const tableMetadata = await fetchTableMetadata(schemaId, tableId);

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata.label || tableMetadata.id, url: "" },
];

const currentBreadCrumb = computed(
  () => tableMetadata.label ?? tableMetadata.id
);

const keyColumns =
  tableMetadata.columns?.filter((c: IColumn) => c.key === 1) || [];
const firstKeyColumn = keyColumns[0];

const displayConfig: IDisplayConfig = {
  layout: "table",
  showLayoutToggle: true,
  showFilters: true,
  pageSize: 10,
  ...(firstKeyColumn && {
    columnConfig: {
      [firstKeyColumn.id]: {
        getHref: (_col: IColumn, row: IRow) => {
          const pk: Record<string, any> = {};
          for (const kc of keyColumns) {
            pk[kc.id] = row[kc.id];
          }
          return `/${schemaId}/view/${tableId}?${encodeRecordId(pk)}`;
        },
      },
    },
  }),
};

const { isAdmin, session } = await useSession(schemaId);
</script>
<template>
  <DetailPageLayout>
    <template #header>
      <PageHeader :title="tableMetadata?.label ?? ''" align="left">
        <template #prefix>
          <BreadCrumbs
            :crumbs="crumbs"
            :current="currentBreadCrumb"
            align="left"
          />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <Emx2DataView
        :schema-id="schemaId"
        :table-id="tableId"
        :config="displayConfig"
        :is-editable="session?.roles?.includes('Editor') || isAdmin"
      />
    </template>
  </DetailPageLayout>
</template>
