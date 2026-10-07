<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type {
  IColumn,
  IRow,
  TableType,
} from "../../../../metadata-utils/src/types.ts";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import { errorToMessage } from "../../../../tailwind-components/app/utils/errorToMessage.ts";
import type {
  CustomRole,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import DeleteRolesConfirmation from "../../components/DeleteRolesConfirmation.vue";
import EditRoleModal from "../../components/EditRoleModal.vue";
import { deleteRoles, getCustomRoles } from "../../util/adminUtils.ts";

definePageMeta({
  middleware: "admin-only",
});

const ROW_KEY_FIELD = "keyString";
const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING" },
  { label: "Users", id: "users", columnType: "STRING" },
];

const TABLE_METADATA = {
  id: "roles",
  schemaId: "roles",
  name: "Roles",
  label: "Roles",
  tableType: "DATA" as TableType,
  columns: COLUMNS,
};

const settings = ref<ITableSettings>({
  page: 1,
  pageSize: constants.PAGE_SIZE_DEFAULT,
  orderby: { column: "", direction: "ASC" },
  search: "",
  orderedColumnsIds: [],
});

const customRoles = ref<CustomRole[]>([]);
const selectedRows = ref<string[]>([]);
const showDeleteRoleModal = ref(false);
const deleteErrorMessage = ref("");
const showEditRoleModal = ref(false);

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
  switch (action) {
    case "edit-selection":
      if (selectedRows.value.length === 1) {
        const singleSelectedRow = rows.value.find((row) =>
          selectedRows.value.includes(row[ROW_KEY_FIELD] as string)
        );
        console.log("Edit action for:", singleSelectedRow);
      }
      break;
    case "delete-selection":
      openDeleteRoleModal();
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

async function handleDeleteRoles() {
  const rowsToDelete = rows.value.filter((row) =>
    selectedRows.value.includes(row[ROW_KEY_FIELD] as string)
  );
  try {
    await deleteRoles(rowsToDelete);
  } catch (error) {
    console.error("Error deleting roles", error);
    deleteErrorMessage.value = errorToMessage(error, "Error deleting roles");
    return;
  }
  showDeleteRoleModal.value = false;
  selectedRows.value = [];
  await loadCustomRoles();
}

function openDeleteRoleModal() {
  deleteErrorMessage.value = "";
  showDeleteRoleModal.value = true;
}
</script>

<template>
  <TableInteractive
    searchPlaceholder="Search roles"
    :rowIdKey="ROW_KEY_FIELD"
    :rows="paginatedRows"
    :rowCount="rows.length"
    :settings="settings"
    :selectedRows="selectedRows"
    :tableMetadata="TABLE_METADATA"
    @toggleRowSelection="handleRowSelection"
    @rowAction="handleRowAction"
    @update:settings="handleSettingsChange"
  >
    <template #buttons>
      <Button @click="showEditRoleModal = true">Add Role</Button>
    </template>
  </TableInteractive>

  <DeleteRolesConfirmation
    v-if="showDeleteRoleModal"
    :selectedRoles="selectedRows"
    :errorMessage="deleteErrorMessage"
    v-model:visible="showDeleteRoleModal"
    @deleteRoles="handleDeleteRoles"
  />

  <EditRoleModal v-model:visible="showEditRoleModal"></EditRoleModal>
</template>
