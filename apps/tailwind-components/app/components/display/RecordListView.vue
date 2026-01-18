<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  IRefColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import InlinePagination from "./InlinePagination.vue";
import { rowToString } from "../../utils/rowToString";

const props = defineProps<{
  rows: IRow[];
  refColumn: IRefColumn;
  page: number;
  pageSize: number;
  totalCount: number;
  getRefClickAction?: (col: IColumn, row: IRow) => () => void;
}>();

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const totalPages = computed(() => Math.ceil(props.totalCount / props.pageSize));
const showPagination = computed(() => props.totalCount > props.pageSize);

function handleClick(row: IRow) {
  if (props.getRefClickAction) {
    props.getRefClickAction(props.refColumn, row)();
  }
}

function getLabel(row: IRow): string {
  return (
    rowToString(row, props.refColumn.refLabel) ||
    String(row.name || row.id || "")
  );
}
</script>

<template>
  <div class="record-list-view">
    <ul v-if="rows.length" class="space-y-1.5 list-none p-0 m-0">
      <li v-for="(row, index) in rows" :key="index">
        <a
          href="#"
          class="text-link hover:text-link-hover hover:underline transition-colors"
          @click.prevent="handleClick(row)"
        >
          {{ getLabel(row) }}
        </a>
      </li>
    </ul>
    <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
    <InlinePagination
      v-if="showPagination"
      :current-page="page"
      :total-pages="totalPages"
      @update:page="emit('update:page', $event)"
    />
  </div>
</template>
