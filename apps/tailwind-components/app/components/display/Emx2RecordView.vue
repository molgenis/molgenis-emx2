<script setup lang="ts">
import { computed, watch, type Component } from "vue";
import { useAsyncData } from "#app";
import type {
  IColumn,
  ITableMetaData,
  IRow,
} from "../../../../metadata-utils/src/types";
import RecordView from "./RecordView.vue";
import LoadingContent from "../LoadingContent.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowData from "../../composables/fetchRowData";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    rowId: Record<string, any>;
    viewColumns?: string[];
    columnOverrides?: Map<string, Component>;
    getRefClickAction?: (col: IColumn, row: IRow) => () => void;
    showEmpty?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

const {
  data: metadata,
  status: metadataStatus,
  error: metadataError,
  refresh: refreshMetadata,
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

// Combined status for loading state
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

// Process metadata: filter by viewColumns and apply columnOverrides
const processedMetadata = computed<ITableMetaData | undefined>(() => {
  if (!metadata.value) return undefined;

  let columns = [...metadata.value.columns];

  // Filter and order by viewColumns if provided
  if (props.viewColumns && props.viewColumns.length > 0) {
    const viewColumnSet = new Set(props.viewColumns);
    // Keep sections/headings that contain visible columns
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

    // Order by viewColumns (preserve section/heading positions)
    const orderMap = new Map(props.viewColumns.map((id, idx) => [id, idx]));
    columns.sort((a, b) => {
      const aOrder = orderMap.get(a.id) ?? Number.MAX_SAFE_INTEGER;
      const bOrder = orderMap.get(b.id) ?? Number.MAX_SAFE_INTEGER;
      return aOrder - bOrder;
    });
  }

  // Apply columnOverrides if provided
  if (props.columnOverrides) {
    columns = columns.map((col) => {
      const override = props.columnOverrides?.get(col.id);
      if (override) {
        return { ...col, displayComponent: override } as IColumn & {
          displayComponent: Component;
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

// Refresh both data sources
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
      :get-ref-click-action="getRefClickAction"
    >
      <template #header>
        <slot name="header" />
      </template>
      <template #footer>
        <slot name="footer" />
      </template>
    </RecordView>
  </LoadingContent>
</template>
