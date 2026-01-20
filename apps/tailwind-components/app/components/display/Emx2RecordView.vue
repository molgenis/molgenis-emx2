<script setup lang="ts">
import { computed, watch } from "vue";
import { useAsyncData } from "#app";
import type {
  IColumn,
  IRefColumn,
  ITableMetaData,
  IDisplayConfig,
} from "../../../../metadata-utils/src/types";
import RecordView from "./RecordView.vue";
import Emx2ListView from "./Emx2ListView.vue";
import LoadingContent from "../LoadingContent.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowData from "../../composables/fetchRowData";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    rowId: Record<string, any>;
    viewColumns?: string[];
    displayConfig?: Map<string, IDisplayConfig>;
    showEmpty?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

function buildRefbackFilter(column: IColumn, rowId: Record<string, any>) {
  const refCol = column as IRefColumn;
  if (refCol.columnType === "REFBACK" && refCol.refBackId) {
    return { [refCol.refBackId]: { equals: rowId } };
  }
  return undefined;
}

const {
  data: metadata,
  status: metadataStatus,
  error: metadataError,
} = useAsyncData(
  `metadata-${props.schemaId}-${props.tableId}`,
  () => fetchTableMetadata(props.schemaId, props.tableId),
  { watch: [() => props.schemaId, () => props.tableId] }
);

const {
  data: rowData,
  status: rowStatus,
  error: rowError,
  refresh: refreshRow,
} = useAsyncData(
  `row-${props.schemaId}-${props.tableId}-${JSON.stringify(props.rowId)}`,
  () => fetchRowData(props.schemaId, props.tableId, props.rowId),
  { watch: [() => props.schemaId, () => props.tableId, () => props.rowId] }
);

const combinedStatus = computed(() => {
  if (metadataStatus.value === "pending" || rowStatus.value === "pending") {
    return "pending";
  }
  if (metadataStatus.value === "error" || rowStatus.value === "error") {
    return "error";
  }
  return "success";
});

const errorText = computed(() => {
  if (metadataError.value) {
    return `Failed to load metadata: ${metadataError.value.message}`;
  }
  if (rowError.value) {
    return `Failed to load row data: ${rowError.value.message}`;
  }
  return undefined;
});

const processedMetadata = computed<ITableMetaData | undefined>(() => {
  if (!metadata.value) return undefined;

  let columns = [...metadata.value.columns];

  if (props.viewColumns && props.viewColumns.length > 0) {
    const viewColumnSet = new Set(props.viewColumns);
    const visibleSections = new Set<string>();
    const visibleHeadings = new Set<string>();

    columns.forEach((col) => {
      if (viewColumnSet.has(col.id)) {
        if (col.section) visibleSections.add(col.section);
        if (col.heading) visibleHeadings.add(col.heading);
      }
    });

    columns = columns.filter((col) => {
      if (col.columnType === "SECTION") {
        return visibleSections.has(col.id);
      }
      if (col.columnType === "HEADING") {
        return visibleHeadings.has(col.id);
      }
      return viewColumnSet.has(col.id);
    });

    const orderMap = new Map(props.viewColumns.map((id, idx) => [id, idx]));
    columns.sort((a, b) => {
      const aOrder = orderMap.get(a.id) ?? Number.MAX_SAFE_INTEGER;
      const bOrder = orderMap.get(b.id) ?? Number.MAX_SAFE_INTEGER;
      return aOrder - bOrder;
    });
  }

  if (props.displayConfig) {
    columns = columns.map((col) => {
      const config = props.displayConfig?.get(col.id);
      if (config) {
        return {
          ...col,
          displayConfig: { ...col.displayConfig, ...config },
        };
      }
      return col;
    });
  }

  return {
    ...metadata.value,
    columns,
  };
});

watch(
  () => props.rowId,
  () => {
    refreshRow();
  },
  { deep: true }
);
</script>

<template>
  <LoadingContent
    :id="`emx2-record-view-${schemaId}-${tableId}`"
    :status="combinedStatus"
    loading-text="Loading record..."
    :error-text="errorText"
    :show-slot-on-error="false"
  >
    <RecordView
      v-if="processedMetadata && rowData"
      :metadata="processedMetadata"
      :row="rowData"
      :show-empty="showEmpty"
    >
      <template #list="{ column, value }">
        <Emx2ListView
          :schema-id="(column as IRefColumn).refSchemaId || schemaId"
          :table-id="(column as IRefColumn).refTableId!"
          :filter="buildRefbackFilter(column, rowId)"
          :show-search="false"
          :paging-limit="5"
          :ref-label="(column as IRefColumn).refLabel || (column as IRefColumn).refLabelDefault"
          :click-action="column.displayConfig?.clickAction"
        />
      </template>
      <template #header>
        <slot name="header" />
      </template>
      <template #footer>
        <slot name="footer" />
      </template>
    </RecordView>
  </LoadingContent>
</template>
