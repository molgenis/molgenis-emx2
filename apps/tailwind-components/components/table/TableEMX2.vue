<script setup lang="ts">
import type { ITableSettings, sortDirection } from "~/types/types";
import type { IColumn } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    tableId: string;
    columns: IColumn[];
    rows: Record<string, any>[];
    count: number;
    settings?: ITableSettings;
  }>(),
  {
    settings: {
      //@ts-ignore
      tableId: "",
      page: 1,
      pageSize: 10,
      orderby: { column: "", direction: "ASC" },
      search: "",
    },
  }
);

const emit = defineEmits(["update:settings"]);
const mgAriaSortMappings = {
  ASC: "ascending",
  DESC: "descending",
};

function handleSortRequest(columnId: string) {
  let direction: sortDirection = "ASC";
  if (props.settings.orderby.column === columnId) {
    direction = props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";
  }

  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction },
  });
}

function handleSearchRequest(search: string) {
  emit("update:settings", {
    ...props.settings,
    search,
  });
}

function handlePagingRequest(page: number) {
  emit("update:settings", {
    ...props.settings,
    page,
  });
}
</script>
<template>
  <div class="flex pb-[30px]">
    <FilterSearch
      class="w-2/5"
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    >
    </FilterSearch>
  </div>

  <div class="overflow-auto rounded-b-50px">
    <div
      class="overflow-x-auto overscroll-x-contain bg-table rounded-t-3px pb-6"
    >
      <table class="text-left table-fixed w-full">
        <thead>
          <tr>
            <th
              v-for="column in columns"
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
              v-for="column in columns"
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
