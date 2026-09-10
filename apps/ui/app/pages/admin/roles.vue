<script setup lang="ts">
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import DisplayRecordsTable from "../../../../tailwind-components/app/components/display/records/Table.vue";

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
</script>

<template>
  <DisplayRecordsTable
    :rows="rows"
    :columns="COLUMNS"
    :titleTemplate="'Roles'"
  />
</template>
