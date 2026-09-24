<script setup lang="ts">
import { computed, ref } from "vue";
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

const sortedRows = computed(() => {
  const { column, direction } = settings.value.orderby;
  return [...rows].sort((a: Record<string, any>, b: Record<string, any>) => {
    if (a[column] < b[column]) return direction === "ASC" ? -1 : 1;
    if (a[column] > b[column]) return direction === "ASC" ? 1 : -1;
    return 0;
  });
});

const settings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: {
    column: "name",
    direction: "ASC",
  },
  orderedColumnsIds: [],
});
</script>

<template>
  <TableInteractive
    :columns="columns"
    :rows="sortedRows"
    :count="rows.length"
    :settings="settings"
    @update:settings="(value) => (settings = value)"
  />
</template>
