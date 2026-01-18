<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  IRefColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import InlinePagination from "./InlinePagination.vue";
import ValueRef from "../value/Ref.vue";

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

function handleRefClick(row: IRow) {
  if (props.getRefClickAction) {
    props.getRefClickAction(props.refColumn, row)();
  }
}
</script>

<template>
  <div class="record-list-view">
    <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
      <li v-for="(row, index) in rows" :key="index">
        <ValueRef
          :metadata="refColumn"
          :data="row"
          @refCellClicked="handleRefClick(row)"
        />
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
