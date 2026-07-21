<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";

interface IColumnAction {
  getHref?: (col: IColumn, row: IRow) => string;
  clickAction?: (col: IColumn, row: IRow) => void;
}
import ValueEMX2 from "../value/EMX2.vue";
import { useRecordNavigation } from "../../composables/useRecordNavigation";

const props = defineProps<{
  columns: IColumn[];
  rows: IRow[];
  columnConfig?: Record<string, IColumnAction>;
  schemaId?: string;
  tableId?: string;
}>();

defineSlots<{
  actions?: (props: { row: IRow }) => any;
}>();

function getColumnConfig(colId: string): IColumnAction | undefined {
  return props.columnConfig?.[colId];
}

function rowKey(row: IRow, index: number): string {
  const pkCol = props.columns.find((c) => c.key === 1);
  if (pkCol && row[pkCol.id] != null) {
    return String(row[pkCol.id]);
  }
  return String(index);
}

const linkColumnIndex = computed(() => {
  const titleIndex = props.columns.findIndex((c) => c.role === "TITLE");
  if (titleIndex !== -1) return titleIndex;
  const keyIndex = props.columns.findIndex((c) => c.key === 1);
  return keyIndex;
});

const linkColumnConfig = computed(() => {
  const idx = linkColumnIndex.value;
  if (idx === -1) return undefined;
  const col = props.columns[idx];
  return col ? getColumnConfig(col.id) : undefined;
});

const isRowClickable = computed(() => !!props.schemaId && !!props.tableId);

const { navigateToRecord } = useRecordNavigation();

async function handleRowClick(row: IRow) {
  if (!props.schemaId || !props.tableId) return;
  navigateToRecord(props.schemaId, props.tableId, row);
}
</script>

<template>
  <div class="overflow-x-auto overscroll-x-contain">
    <table class="w-full table-auto">
      <thead>
        <tr>
          <th
            v-for="(col, colIndex) in columns"
            :key="col.id"
            :class="[
              'py-2.5 px-2.5 text-left border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5',
              colIndex > 0 ? 'hidden sm:table-cell' : '',
            ]"
          >
            {{ col.label || col.id }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, rowIndex) in rows"
          :key="rowKey(row, rowIndex)"
          class="sm:hover:bg-hover"
        >
          <td
            v-for="(col, colIndex) in columns"
            :key="col.id"
            :class="[
              'py-2.5 px-2.5 border-b border-gray-200 text-table-row first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5',
              colIndex > 0 ? 'hidden sm:table-cell' : '',
            ]"
          >
            <div
              v-if="colIndex === linkColumnIndex"
              class="flex items-center gap-2"
            >
              <slot name="actions" :row="row" />
              <NuxtLink
                v-if="linkColumnConfig?.getHref"
                :to="linkColumnConfig.getHref(col, row)"
                class="font-bold text-link hover:underline truncate"
              >
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </NuxtLink>
              <span
                v-else-if="linkColumnConfig?.clickAction"
                class="font-bold text-link hover:underline cursor-pointer truncate"
                @click="linkColumnConfig.clickAction(col, row)"
              >
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </span>
              <span
                v-else-if="isRowClickable"
                class="font-bold text-link hover:underline cursor-pointer truncate"
                @click="handleRowClick(row)"
              >
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </span>
              <span v-else class="font-bold">
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </span>
            </div>
            <ValueEMX2 v-else :metadata="col" :data="row[col.id]" />
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td
            :colspan="columns.length"
            class="py-4 px-3 text-center text-body-base opacity-60 italic"
          >
            No items
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
