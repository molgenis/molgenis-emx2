<script setup lang="ts">
import { computed, ref } from "vue";
import type { IRow } from "../../../../metadata-utils/src/types";
import type { ITableSettings } from "../../../types/types";
import TableInteractive from "../../components/table/TableInteractive.vue";

const columns = [
  { id: "name", label: "Name" },
  { id: "email", label: "Email" },
  { id: "role", label: "Role" },
];

const rows: IRow[] = [
  { name: "John Doe", email: "john.doe@example.com", role: "User" },
  { name: "Jane Smith", email: "jane.smith@example.com", role: "Admin" },
  { name: "Alice Johnson", email: "alice.johnson@example.com", role: "User" },
  { name: "Bob Brown", email: "bob.brown@example.com", role: "Moderator" },
  { name: "Charlie Davis", email: "charlie.davis@example.com", role: "User" },
  { name: "David Evans", email: "david.evans@example.com", role: "Admin" },
  { name: "Eve Foster", email: "eve.foster@example.com", role: "User" },
  { name: "Frank Green", email: "frank.green@example.com", role: "Moderator" },
  { name: "Grace Harris", email: "grace.harris@example.com", role: "User" },
  { name: "Hannah Ingram", email: "hannah.ingram@example.com", role: "Admin" },
  { name: "Ian Johnson", email: "ian.johnson@example.com", role: "User" },
  { name: "Jack King", email: "jack.king@example.com", role: "Moderator" },
];

const appliedRows = computed(() => {
  const { column, direction } = settings.value.orderby;

  const sorted = [...rows].sort(
    (a: Record<string, any>, b: Record<string, any>) => {
      if (a[column] < b[column]) return direction === "ASC" ? -1 : 1;
      if (a[column] > b[column]) return direction === "ASC" ? 1 : -1;
      return 0;
    }
  );

  const search = settings.value.search?.toLowerCase() || "";
  const searchedRows = sorted.filter((row) =>
    Object.values(row).some((value) =>
      String(value).toLowerCase().includes(search)
    )
  );

  return searchedRows;
});

const slicedRows = computed(() => {
  const startIndex = (settings.value.page - 1) * settings.value.pageSize;
  const endIndex = startIndex + settings.value.pageSize;
  return appliedRows.value.slice(startIndex, endIndex);
});

const rowCount = computed(() => appliedRows.value.length);

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
    :rows="slicedRows"
    :rowCount="rowCount"
    :settings="settings"
    @update:settings="(value) => (settings = value)"
  />
</template>
