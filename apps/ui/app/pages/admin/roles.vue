<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types.ts";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type {
  CustomRole,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import { getCustomRoles } from "../../util/adminUtils.ts";

definePageMeta({
  middleware: "admin-only",
});

const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING_ARRAY" },
  { label: "Users", id: "users", columnType: "STRING_ARRAY" },
];

const settings = ref<ITableSettings>({
  page: 1,
  pageSize: constants.PAGE_SIZE_DEFAULT,
  orderby: { column: "", direction: "ASC" },
  search: "",
  orderedColumnsIds: [],
});
const ROW_KEY_FIELD = "keyString";

const customRoles = ref<CustomRole[]>([]);
const selectedRows = ref<string[]>([]);

let latestRequest = 0;
await loadCustomRoles();

const rows = computed<IRow[]>(() => {
  const transformedRoles = customRoles.value.map((customRole) => ({
    schemaId: customRole.schemaId,
    roleName: customRole.roleName,
    keyString: `${customRole.schemaId}/${customRole.roleName}`,
    tables: getTableNames(customRole),
    users: getUserNames(customRole),
  }));

  const sortedRoles = transformedRoles.sort(
    (a: Record<string, string>, b: Record<string, string>) => {
      const { column, direction } = settings.value.orderby;
      if (!column) return 0;

      const aValue = a[column]?.toLowerCase() ?? "";
      const bValue = b[column]?.toLowerCase() ?? "";

      if (aValue < bValue) return direction === "ASC" ? -1 : 1;
      if (aValue > bValue) return direction === "ASC" ? 1 : -1;
      return 0;
    }
  );
  const filteredRoles = sortedRoles.filter((role) => {
    const search = settings.value?.search?.toLowerCase() || "";
    return (
      role.schemaId.toLowerCase().includes(search) ||
      role.roleName.toLowerCase().includes(search) ||
      role.tables.toLowerCase().includes(search) ||
      role.users.toLowerCase().includes(search)
    );
  });

  return filteredRoles;
});

const paginatedRows = computed(() => {
  const start = (settings.value.page - 1) * settings.value.pageSize;
  const end = start + settings.value.pageSize;
  return rows.value.slice(start, end);
});

async function loadCustomRoles() {
  const request = ++latestRequest;
  const loaded = await getCustomRoles();
  if (request === latestRequest) {
    customRoles.value = loaded.customRoles;
  }
}

async function handleSettingsChange(updated: ITableSettings) {
  settings.value = updated;
  await loadCustomRoles();
}

function handleRowSelection(row: IRow) {
  const rowKey = row[ROW_KEY_FIELD] as string;
  const index = selectedRows.value.indexOf(rowKey);
  if (index !== -1) {
    selectedRows.value.splice(index, 1);
  } else {
    selectedRows.value.push(rowKey);
  }
}

function handleRowAction(payload: { action: string }) {
  const action = payload.action;
  const singleRowSelected =
    selectedRows.value.length === 1
      ? rows.value.find((row) =>
          selectedRows.value.includes(row[ROW_KEY_FIELD] as string)
        )
      : null;
  switch (action) {
    case "edit-selection":
      if (singleRowSelected) {
        // Handle edit action for the selected row
        console.log("Edit action for:", singleRowSelected);
      }
      break;
    case "delete-selection":
      if (singleRowSelected) {
        // Handle delete action for the selected row
        console.log("Delete action for:", singleRowSelected);
      } else {
        console.log("Delete action for multiple rows:", selectedRows.value);
      }
      break;
    case "select-all-on-page":
      paginatedRows.value.forEach((row) => {
        const rowKey = row[ROW_KEY_FIELD] as string;
        if (!selectedRows.value.includes(rowKey)) {
          selectedRows.value.push(rowKey);
        }
      });
      break;
    case "select-none":
      selectedRows.value = [];
      break;
    default:
      console.warn("Unknown action:", action);
  }
}

function getTableNames(customRole: CustomRole) {
  return customRole.permissions
    .map((permission) => permission.table)
    .join(", ");
}

function getUserNames(customRole: CustomRole) {
  return customRole.users.join(", ");
}
</script>

<template>
  <TableInteractive
    searchPlaceholder="Search roles"
    :rowIdKey="ROW_KEY_FIELD"
    :columns="COLUMNS"
    :rows="paginatedRows"
    :rowCount="rows.length"
    :settings="settings"
    :selectedRows="selectedRows"
    @toggleRowSelection="handleRowSelection"
    @rowAction="handleRowAction"
    @update:settings="handleSettingsChange"
  />
</template>
