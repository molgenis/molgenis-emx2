<script setup lang="ts">
import { computed } from "vue";
import { navigateTo } from "#imports";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";

interface IColumnAction {
  getHref?: (col: IColumn, row: IRow) => string;
  clickAction?: (col: IColumn, row: IRow) => void;
}
import ValueEMX2 from "../value/EMX2.vue";
import { getPrimaryKey } from "../../utils/getPrimaryKey";
import { buildRefHref } from "../../utils/displayUtils";

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

const firstColumnConfig = computed(() => {
  const firstCol = props.columns[0];
  return firstCol ? getColumnConfig(firstCol.id) : undefined;
});

const firstColumnIsKey = computed(
  () => props.columns.length > 0 && props.columns[0]?.key === 1
);

const isRowClickable = computed(() => !!props.schemaId && !!props.tableId);

async function handleRowClick(row: IRow) {
  if (!props.schemaId || !props.tableId) return;
  const rowKey = await getPrimaryKey(row, props.tableId, props.schemaId);
  const href = buildRefHref(props.schemaId, props.tableId, undefined, rowKey);
  navigateTo(href);
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
          class="sm:hover:bg-hover sm:hover:cursor-pointer"
          @click="isRowClickable ? handleRowClick(row) : undefined"
        >
          <td
            v-for="(col, colIndex) in columns"
            :key="col.id"
            :class="[
              'py-2.5 px-2.5 border-b border-gray-200 text-table-row first:font-bold first:text-link first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5',
              colIndex > 0 ? 'hidden sm:table-cell' : '',
            ]"
          >
            <div
              v-if="colIndex === 0 && firstColumnIsKey"
              class="flex items-center gap-2"
            >
              <slot name="actions" :row="row" />
              <NuxtLink
                v-if="firstColumnConfig?.getHref"
                :to="firstColumnConfig.getHref(col, row)"
                class="text-link hover:underline truncate"
              >
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </NuxtLink>
              <span
                v-else-if="firstColumnConfig?.clickAction"
                class="text-link hover:underline cursor-pointer truncate"
                @click="firstColumnConfig.clickAction(col, row)"
              >
                <ValueEMX2 :metadata="col" :data="row[col.id]" />
              </span>
              <ValueEMX2 v-else :metadata="col" :data="row[col.id]" />
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
