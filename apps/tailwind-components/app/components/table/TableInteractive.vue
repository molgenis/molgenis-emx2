<script setup lang="ts">
import { useDebounceFn } from "@vueuse/core";
import { computed } from "vue";
import type {
  columnValue,
  IRow,
} from "../../../../metadata-utils/src/types.ts";
import type { ITableSettings } from "../../../types/types.ts";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import Checkbox from "../input/Checkbox.vue";
import InputSearch from "../input/Search.vue";
import Pagination from "../Pagination.vue";
import Table from "../Table.vue";
import TableCell from "../TableCell.vue";
import TableRow from "../TableRow.vue";
import RowControls from "./control/RowControls.vue";
import TableHeadCell from "./TableHeadCell.vue";
import TableHeaderAction from "./TableHeaderAction.vue";

const props = withDefaults(
  defineProps<{
    columns: { id: string; label: string }[];
    rows: IRow[];
    rowCount: number;
    rowIdKey: string;
    settings: ITableSettings;
    selectedRows?: Map<string, Record<string, columnValue>>;
    searchPlaceholder?: string;
  }>(),
  {
    searchPlaceholder: "Search",
  }
);

const numberOfSelectedRows = computed(() => props.selectedRows?.size || 0);

const emit = defineEmits<{
  (event: "update:settings", value: ITableSettings): void;
  (event: "rowAction", payload: { action: string }): void;
  (event: "toggleRowSelection", row: IRow): void;
}>();

const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.rowCount / props.settings.pageSize))
);

function handlePagingRequest(page: number) {
  emit("update:settings", { ...props.settings, page });
}

function handlePageSizeChange(pageSize: string) {
  emit("update:settings", {
    ...props.settings,
    pageSize: Number.parseInt(pageSize),
    page: 1,
  });
}

const handleSearchChange = useDebounceFn((search?: string) => {
  emit("update:settings", { ...props.settings, search, page: 1 });
}, FILTER_DEBOUNCE);

function handleSortRequest(columnId: string) {
  const isSameColumn = props.settings.orderby.column === columnId;
  const newDirection =
    isSameColumn && props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";

  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction: newDirection },
  });
}

function handleRowAction(payload: { action: string }) {
  emit("rowAction", payload);
}

function toggleRowSelection(row: IRow) {
  emit("toggleRowSelection", row);
}

function getRowKey(row: IRow): string {
  return row[props.rowIdKey] as string; //FIXME
}
</script>

<template>
  <div class="flex mb-[30px] justify-between h-50px">
    <RowControls
      :numberOfSelectedRows="numberOfSelectedRows"
      :allRowsSelected="
        numberOfSelectedRows === Math.min(settings.pageSize, rows.length)
      "
      :canUpdate="true"
      :canDelete="true"
      :canModifySelection="true"
      @rowAction="handleRowAction"
    />
    <InputSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      size="medium"
      :modelValue="settings.search"
      @update:modelValue="handleSearchChange($event)"
      :placeholder="searchPlaceholder"
      id="search-input"
    />
    <slot name="buttons" />
  </div>
  <Table>
    <template #head>
      <TableHeadRow>
        <TableHeadCell class="left-0 bg-table z-20 w-12"> </TableHeadCell>
        <TableHeadCell v-for="column in columns" :key="column.id">
          <TableHeaderAction
            :column="column"
            :settings="settings"
            @sortRequested="handleSortRequest"
          />
        </TableHeadCell>
      </TableHeadRow>
    </template>
    <template #body>
      <TableRow v-for="(row, index) in rows" :key="index">
        <TableCell
          class="left-0 bg-table group-hover:bg-hover z-10 w-12 h-13 py-0 px-2.5"
        >
          <div class="flex justify-center items-center h-full">
            <Checkbox
              :modelValue="props.selectedRows?.has(getRowKey(row))"
              @update:modelValue="toggleRowSelection(row)"
            />
          </div>
        </TableCell>
        <TableCell v-for="column in columns" :key="column.id">
          {{ row[column.id] }}
        </TableCell>
      </TableRow>
    </template>
    <template #foot> </template>
  </Table>
  <Pagination
    class="pt-0 pb-[30px]"
    :currentPage="settings.page"
    :totalPages="totalPages"
    :jumpToEdge="true"
    :pageSize="settings.pageSize"
    :showPageSizeSelector="true"
    @update="handlePagingRequest($event)"
    @update:pageSize="handlePageSizeChange($event)"
  />
</template>
