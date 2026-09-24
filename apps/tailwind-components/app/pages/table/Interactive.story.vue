<script setup lang="ts">
import TableInteractive from "~/components/table/TableInteractive.vue";
import type { ITableSettings } from "~~/types/types";

const columns = [
  { id: "name", label: "Name" },
  { id: "email", label: "Email" },
  { id: "role", label: "Role" },
];
const rows = [
  { name: "John Doe", email: "john.doe@example.com", role: "User" },
  { name: "Jane Smith", email: "jane.smith@example.com", role: "Admin" },
];
const settings: ITableSettings = {
  page: 1,
  pageSize: 10,
  orderby: {
    column: "name",
    direction: "ASC",
  },
  orderedColumnsIds: [],
};
</script>

<template>
  <TableInteractive
    :columns="columns"
    :rows="rows"
    :count="rows.length"
    :settings="settings"
    @update:settings="(value) => (settings = value)"
  />
</template>
