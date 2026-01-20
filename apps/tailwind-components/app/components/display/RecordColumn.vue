<script setup lang="ts">
import { ref, computed, useSlots } from "vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import RecordTableView from "./RecordTableView.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    value: any;
    showEmpty?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

const slots = useSlots();

const listPage = ref(1);
const defaultPageSize = 5;

const displayComponent = computed(() => {
  const comp = props.column.displayConfig?.component;
  return typeof comp === "string" ? comp : comp ? "custom" : "bullets";
});

const effectivePageSize = computed(() => {
  return props.column.displayConfig?.pageSize || defaultPageSize;
});

const visibleColumns = computed(() => {
  const config = props.column.displayConfig;
  if (!config?.visibleColumns) return [];
  const refCol = props.column as IRefColumn;
  if (!refCol.refTableMetadata?.columns) return [];
  return refCol.refTableMetadata.columns.filter((c) =>
    config.visibleColumns!.includes(c.id)
  );
});

const isRefBack = computed(() => props.column.columnType === "REFBACK");

const useTableMode = computed(() => {
  return (
    (props.column.columnType === "REF_ARRAY" || isRefBack.value) &&
    displayComponent.value === "table" &&
    visibleColumns.value.length > 0 &&
    !slots.list
  );
});

const allRows = computed(() => (Array.isArray(props.value) ? props.value : []));

const visibleRows = computed(() =>
  allRows.value.slice(
    (listPage.value - 1) * effectivePageSize.value,
    listPage.value * effectivePageSize.value
  )
);

function isEmpty(val: any): boolean {
  if (val === null || val === undefined || val === "") return true;
  if (Array.isArray(val) && val.length === 0) return true;
  return false;
}
</script>

<template>
  <template v-if="isEmpty(value) && !showEmpty"></template>
  <span v-else-if="isEmpty(value) && showEmpty" class="text-gray-400 italic">
    not provided
  </span>
  <!-- REFBACK: use #list slot if provided (smart fetch), else delegate to ValueEMX2 -->
  <slot
    v-else-if="isRefBack && $slots.list"
    name="list"
    :column="column"
    :value="value"
  />
  <!-- Table mode for REF_ARRAY/REFBACK with displayConfig.component='table' (with pagination) -->
  <RecordTableView
    v-else-if="useTableMode"
    :rows="visibleRows"
    :columns="visibleColumns"
    :ref-column="column as IRefColumn"
    v-model:page="listPage"
    :page-size="effectivePageSize"
    :total-count="allRows.length"
  />
  <!-- Everything else: delegate to ValueEMX2 -->
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
