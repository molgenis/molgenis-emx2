<script setup lang="ts">
import type { ITableSettings, sortDirection } from "~/types/types";
import type { IColumn } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    rows: Record<string, any>[];
    count: number;
    settings?: ITableSettings;
  }>(),
  {
    settings: {
      //@ts-ignore
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
  if (props.settings?.orderby?.column === columnId) {
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
  <div class="flex">
    <FilterSearch
      class="w-2/5"
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    >
    </FilterSearch>
  </div>
  <div class="overflow-x-auto overscroll-x-contain">
    <table class="text-left table-fixed w-full">
      <thead>
        <tr class="">
          <th
            v-for="column in columns"
            class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left w-64"
            :ariaSort="
              settings?.orderby?.column === column.id
                ? mgAriaSortMappings[settings.orderby.direction]
                : 'none'
            "
            scope="col"
          >
            <button
              class="overflow-ellipsis whitespace-nowrap overflow-hidden hover:cursor-pointer"
              @click="handleSortRequest(column.id)"
            >
              {{ column.label }}
              <ArrowUp
                v-if="
                  column.id === settings?.orderby?.column &&
                  settings?.orderby?.direction === 'ASC'
                "
                class="w-4 h-4 inline-block"
              />
              <ArrowDown
                v-if="
                  column.id === settings?.orderby?.column &&
                  settings?.orderby?.direction === 'DESC'
                "
                class="w-4 h-4 inline-block"
              />
            </button>
          </th>
        </tr>
      </thead>
      <tbody>
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
  <div class="pt-8">
    Showing <span>{{ rows.length }}</span> of <span>{{ count }}</span>
  </div>
  <Pagination
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    @update="handlePagingRequest($event)"
  />
</template>
