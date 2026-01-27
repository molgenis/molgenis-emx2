<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import InlinePagination from "./InlinePagination.vue";
import ValueEMX2 from "../value/EMX2.vue";

const props = defineProps<{
  rows: IRow[];
  columns: IColumn[];
  page: number;
  pageSize: number;
  totalCount: number;
}>();

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const totalPages = computed(() => Math.ceil(props.totalCount / props.pageSize));
const showPagination = computed(() => props.totalCount > props.pageSize);

function rowKey(row: IRow, index: number): string {
  const pkCol = props.columns.find((c) => c.key === 1);
  if (pkCol && row[pkCol.id] != null) {
    return String(row[pkCol.id]);
  }
  return String(index);
}
</script>

<template>
  <div class="record-table-view">
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
            :key="rowKey(row, index)"
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
    <InlinePagination
      v-if="showPagination"
      :current-page="page"
      :total-pages="totalPages"
      @update:page="emit('update:page', $event)"
    />
  </div>
</template>
