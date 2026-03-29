<script setup lang="ts">
import { computed, type Component } from "vue";
import { useAsyncData } from "#app";
import type { IColumnDisplay, IRecordViewConfig } from "../../../types/types";
import RecordView from "./RecordView.vue";
import LoadingContent from "../LoadingContent.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowData from "../../composables/fetchRowData";
import { isRefArrayColumn } from "../../utils/displayUtils";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  rowId: Record<string, any>;
  config?: IRecordViewConfig;
  displayMap?: Record<string, Component>;
}>();

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

const processedColumns = computed<IColumnDisplay[]>(() => {
  if (!metadata.value) return [];

  let columns: IColumnDisplay[] = [...metadata.value.columns];

  if (!props.config?.showMgColumns) {
    columns = columns.filter(
      (col) =>
        !col.id.startsWith("mg_") ||
        col.columnType === "SECTION" ||
        col.columnType === "HEADING"
    );
  }

  if (props.config?.extraColumns?.length) {
    columns = [...columns, ...props.config.extraColumns];
  }

  if (props.config?.visibleColumns?.length) {
    const viewColumnSet = new Set(props.config.visibleColumns);
    const visibleSections = new Set<string>();
    const visibleHeadings = new Set<string>();

    columns.forEach((col) => {
      if (viewColumnSet.has(col.id)) {
        if (col.section) visibleSections.add(col.section);
        if (col.heading) visibleHeadings.add(col.heading);
      }
    });

    columns = columns.filter((col) => {
      if (col.columnType === "SECTION") return visibleSections.has(col.id);
      if (col.columnType === "HEADING") return visibleHeadings.has(col.id);
      return viewColumnSet.has(col.id);
    });

    const orderMap = new Map(
      props.config.visibleColumns.map((id, idx) => [id, idx])
    );
    columns.sort((a, b) => {
      const aOrder = orderMap.get(a.id) ?? Number.MAX_SAFE_INTEGER;
      const bOrder = orderMap.get(b.id) ?? Number.MAX_SAFE_INTEGER;
      return aOrder - bOrder;
    });
  }

  columns = columns.map((col) => {
    const override = props.config?.columnConfig?.[col.id];
    const merged: IColumnDisplay = { ...col, ...override };

    if (!merged.displayComponent && merged.tags?.length && props.displayMap) {
      for (const tag of merged.tags) {
        if (props.displayMap[tag]) {
          merged.displayComponent = props.displayMap[tag];
          break;
        }
      }
    }

    if (
      merged.columnType === "REFBACK" &&
      merged.refTableId &&
      !merged.listConfig
    ) {
      merged.listConfig = {
        layout: "table",
        pageSize: 10,
        showSearch: true,
        hideColumns: merged.refBackId ? [merged.refBackId] : [],
      };
    }

    if (
      isRefArrayColumn(merged.columnType) &&
      merged.refTableId &&
      !merged.listConfig
    ) {
      merged.listConfig = {
        layout: "table",
        pageSize: 10,
        showSearch: true,
      };
    }

    return merged;
  });

  return columns;
});
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
      v-if="processedColumns.length && rowData"
      :columns="processedColumns"
      :data="rowData"
      :show-empty="config?.showEmpty ?? false"
      :show-side-nav="config?.showSideNav"
      :schema-id="schemaId"
      :row-id="rowId"
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
