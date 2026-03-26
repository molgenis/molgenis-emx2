<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";

interface IColumnAction {
  getHref?: (col: IColumn, row: IRow) => string;
  clickAction?: (col: IColumn, row: IRow) => void;
}
import ValueEMX2 from "../value/EMX2.vue";

const COLUMN_WIDTH = 240;

const props = defineProps<{
  columns: IColumn[];
  rows: IRow[];
  columnConfig?: Record<string, IColumnAction>;
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
  () => props.columns.length > 0 && props.columns[0].key === 1
);

const tableWidth = computed(() => `${props.columns.length * COLUMN_WIDTH}px`);
</script>

<template>
  <div class="overflow-x-auto overscroll-x-contain">
    <table class="text-left table-fixed" :style="{ minWidth: tableWidth }">
      <thead>
        <tr class="border-b border-black/10">
          <th
            v-for="(col, colIndex) in columns"
            :key="col.id"
            :class="[
              'px-3 py-2 text-body-sm font-semibold bg-table text-table-column-header whitespace-nowrap',
              colIndex === 0 && firstColumnIsKey
                ? 'sticky left-0 z-10 shadow-[2px_0_4px_-2px_rgba(0,0,0,0.1)]'
                : '',
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
          class="border-b border-black/10 hover:bg-black/5 group"
        >
          <td
            v-for="(col, colIndex) in columns"
            :key="col.id"
            :class="[
              'px-3 py-2 text-body-base bg-table text-table-row whitespace-nowrap overflow-hidden text-ellipsis group-hover:bg-black/5',
              colIndex === 0 && firstColumnIsKey
                ? 'sticky left-0 z-10 shadow-[2px_0_4px_-2px_rgba(0,0,0,0.1)]'
                : '',
            ]"
          >
            <div v-if="colIndex === 0 && firstColumnIsKey" class="flex items-center gap-2">
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
