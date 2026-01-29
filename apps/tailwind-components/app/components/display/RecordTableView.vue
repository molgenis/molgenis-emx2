<script setup lang="ts">
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";

defineProps<{
  rows: IRow[];
  columns: IColumn[];
}>();

function rowKey(row: IRow, index: number, columns: IColumn[]): string {
  const pkCol = columns.find((c) => c.key === 1);
  if (pkCol && row[pkCol.id] != null) {
    return String(row[pkCol.id]);
  }
  return String(index);
}
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-gray-300 dark:border-gray-600">
          <th
            v-for="col in columns"
            :key="col.id"
            class="py-2 px-3 text-left font-semibold text-record-label whitespace-nowrap"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, index) in rows"
          :key="rowKey(row, index, columns)"
          class="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800"
        >
          <td
            v-for="col in columns"
            :key="col.id"
            class="py-2 px-3 text-record-value"
          >
            <ValueEMX2 :metadata="col" :data="row[col.id]" />
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td
            :colspan="columns.length"
            class="py-4 px-3 text-center text-gray-400 dark:text-gray-500 italic"
          >
            No items
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
