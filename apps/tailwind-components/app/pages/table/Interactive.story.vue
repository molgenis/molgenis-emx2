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

const selectedRows = ref<string[]>([]);
const settings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: {
    column: "name",
    direction: "ASC",
  },
  orderedColumnsIds: [],
});

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

function toggleRowSelection(row: IRow) {
  const rowKey = row["email"] as string;
  const index = selectedRows.value.indexOf(rowKey);
  if (index !== -1) {
    selectedRows.value.splice(index, 1);
  } else {
    selectedRows.value.push(rowKey);
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.length === 1
        ? rows.find((row) => selectedRows.value.includes(row.email as string))
        : null;
    switch (action) {
      case "delete-selection":
        if (singleRowSelected) {
          alert(`Delete row: ${singleRowSelected.name}`);
        } else if (selectedRows.value.length) {
          alert(`Delete ${selectedRows.value.length} selected rows`);
        }
        break;
      case "edit-selection":
        if (singleRowSelected) {
          alert(`Edit row: ${singleRowSelected.name}`);
        }
        break;
      case "view-details":
        if (singleRowSelected) {
          alert(`View details for row: ${singleRowSelected.name}`);
        }
        break;
      case "select-all-on-page":
        slicedRows.value.forEach((row) => {
          selectedRows.value.push(row.email as string);
        });
        break;
      case "select-none":
        selectedRows.value = [];
        break;
      case "select-drafts":
        selectedRows.value = [];
        slicedRows.value.forEach((row) => {
          if (row.mg_draft === true) {
            selectedRows.value.push(row.email as string);
          }
        });
        break;
    }
  }
}
</script>

<template>
  <TableInteractive
    :columns="columns"
    :rows="slicedRows"
    :rowCount="rowCount"
    :settings="settings"
    :selectedRows="selectedRows"
    rowIdKey="email"
    @update:settings="(value) => (settings = value)"
    @toggleRowSelection="toggleRowSelection"
    @rowAction="handleRowAction"
  />
</template>
