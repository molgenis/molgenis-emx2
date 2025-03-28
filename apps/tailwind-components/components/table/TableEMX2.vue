<template>
  <div class="flex pb-[30px] justify-between">
    <FilterSearch
      class="w-2/5"
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    >
    </FilterSearch>
    <TableControlColumns
      :columns="props.columns"
      @update:columns="handleColumnsUpdate"
    />
  </div>

  <div class="overflow-auto rounded-b-50px">
    <div
      class="overflow-x-auto overscroll-x-contain bg-table rounded-t-3px pb-6"
    >
      <table class="text-left table-fixed w-full">
        <thead>
          <tr>
            <th
              v-for="column in sortedVisibleColumns"
              class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left w-64"
              :ariaSort="
                settings.orderby.column === column.id
                  ? mgAriaSortMappings[settings.orderby.direction]
                  : 'none'
              "
              scope="col"
            >
              <button
                class="overflow-ellipsis whitespace-nowrap overflow-hidden hover:cursor-pointer text-table-column-header text-body-base capitalize"
                @click="handleSortRequest(column.id)"
              >
                {{ column.label }}
                <ArrowUp
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'ASC'
                  "
                  class="w-4 h-4 inline-block"
                />
                <ArrowDown
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'DESC'
                  "
                  class="w-4 h-4 inline-block"
                />
              </button>
            </th>
          </tr>
        </thead>
        <tbody
          class="mb-3 [&_tr:last-child_td]:border-none [&_tr:last-child_td]:mb-5"
        >
          <tr v-for="row in rows">
            <TableCellTypesEMX2
              v-for="column in sortedVisibleColumns"
              :scope="column.key === 1 ? 'row' : null"
              :metadata="column"
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
import type { ITableSettings, sortDirection } from "../../types/types";
import { sortColumns } from "~/utils/sortColumns";
import type { IColumn } from "../../../metadata-utils/src/types";

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
