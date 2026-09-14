<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import DisplayRecordsTable from "../../../../tailwind-components/app/components/display/records/Table.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type { ITableSettings } from "../../../../tailwind-components/types/types.ts";

const rows: IRow[] = [
  {
    roleName: "Admin",
    schema: "public",
    tables: ["users", "roles", "settings"],
    users: ["user1", "user2"],
  },
  {
    roleName: "Editor",
    schema: "public",
    tables: ["articles", "comments"],
    users: ["user3", "user4"],
  },
  {
    roleName: "Viewer",
    schema: "public",
    tables: ["articles"],
    users: ["user5", "user6"],
  },
];

const COLUMNS: IColumn[] = [
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Schema", id: "schema", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING_ARRAY" },
  { label: "Users", id: "users", columnType: "STRING_ARRAY" },
];

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: constants.PAGE_SIZE_DEFAULT,
    orderby: { column: "", direction: "ASC" },
    search: "",
    orderedColumnsIds: [],
  }),
});

const count = computed(() => rows.length);
const smallestPageSize = computed(() =>
  Math.min(...constants.PAGE_SIZE_OPTIONS)
);

function handlePagingRequest(page: number) {
  settings.value.page = page;
  // refresh();
}

function handlePageSizeChange(pageSize: string) {
  settings.value.pageSize = Number.parseInt(pageSize);
  settings.value.page = 1;
  // refresh();
}
</script>

<template>
  <InputSearch
    class="w-3/5 xl:w-2/5 2xl:w-1/5"
    size="medium"
    v-model="settings.search"
    placeholder="Search roles"
    id="search-input"
  />
  <DisplayRecordsTable
    :rows="rows"
    :columns="COLUMNS"
    :titleTemplate="'Roles'"
  />
  <!-- v-if="count > smallestPageSize" -->
  <Pagination
    class="pt-0 pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    :jump-to-edge="true"
    :page-size="settings.pageSize"
    :show-page-size-selector="true"
    @update="handlePagingRequest($event)"
    @update:pageSize="handlePageSizeChange($event)"
  />
</template>
