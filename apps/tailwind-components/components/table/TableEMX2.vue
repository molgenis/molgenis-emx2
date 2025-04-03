<template>
  <div class="flex pb-[30px] justify-between">
    <FilterSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    >
    </FilterSearch>
    <TableControlColumns
      :columns="columns"
      @update:columns="handleColumnsUpdate"
    />
  </div>

  <div class="overflow-auto rounded-b-theme">
    <div class="overflow-x-auto overscroll-x-contain bg-table rounded-t-theme">
      <table
        class="text-left table-fixed w-full border border-theme border-color-theme"
      >
        <thead>
          <tr>
            <th
              v-for="column in sortedVisibleColumns"
              class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left w-64 overflow-hidden whitespace-nowrap align-middle"
              :ariaSort="
                settings.orderby.column === column.id
                  ? mgAriaSortMappings[settings.orderby.direction]
                  : 'none'
              "
              scope="col"
            >
              <span
                class="whitespace-nowrap max-w-60 w-64 overflow-hidden inline-block"
              >
                <button
                  @click="handleSortRequest(column.id)"
                  class="overflow-ellipsis whitespace-nowrap max-w-56 overflow-hidden inline-block text-left text-table-column-header font-normal align-middle"
                >
                  {{ column.label }}
                </button>
                <ArrowUp
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'ASC'
                  "
                  class="w-4 h-4 inline-block ml-1 text-table-column-header font-normal"
                />
                <ArrowDown
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'DESC'
                  "
                  class="w-4 h-4 inline-block ml-1 text-table-column-header font-normal"
                />
              </span>
            </th>
          </tr>
        </thead>
        <tbody
          class="mb-3 [&_tr:last-child_td]:border-none [&_tr:last-child_td]:mb-5"
        >
          <tr v-for="row in rows">
            <TableCellTypesEMX2
              v-for="column in sortedVisibleColumns"
              class="w-6 text-table-row"
              :scope="column.key === 1 ? 'row' : null"
              :metaData="column"
              :data="row[column.id]"
            />
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <Pagination
    class="pt-[30px] pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    @update="handlePagingRequest($event)"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn } from "../../../metadata-utils/src/types";
import type { ITableSettings, sortDirection } from "../../types/types";
import { sortColumns } from "../../utils/sortColumns";

const mgAriaSortMappings = {
  ASC: "ascending",
  DESC: "descending",
};

const defaultSettings: ITableSettings = {
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
};

const props = defineProps<{
  tableId: string;
  columns: IColumn[];
  rows: Record<string, any>[];
  count: number;
  settings?: ITableSettings;
}>();

const emit = defineEmits(["update:settings", "update:columns"]);

const settings = ref({ ...defaultSettings, ...props.settings });
const columns = ref(props.columns);

const sortedVisibleColumns = computed(() => {
  const visibleColumns = columns.value.filter(
    (column) => column.visible !== "false"
  );
  return sortColumns(visibleColumns);
});

watch(
  () => props.columns,
  (newColumns: IColumn[]) => {
    columns.value = newColumns;
  }
);

function handleSortRequest(columnId: string) {
  const direction: sortDirection = getDirection(columnId);
  settings.value.orderby = { column: columnId, direction };
  emit("update:settings", settings.value);
}

function getDirection(columnId: string): sortDirection {
  if (settings.value.orderby.column === columnId) {
    return settings.value.orderby.direction === "ASC" ? "DESC" : "ASC";
  } else {
    return "ASC";
  }
}

function handleSearchRequest(search: string) {
  settings.value.search = search;
  emit("update:settings", settings.value);
}

function handlePagingRequest(page: number) {
  settings.value.page = page;
  emit("update:settings", settings.value);
}

function handleColumnsUpdate(newColumns: IColumn[]) {
  columns.value = newColumns;
}
</script>
