<script lang="ts" setup>
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type {
  IColumn,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types.ts";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import CellDetailModal from "../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import type {
  cellPayload,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import RoleModal from "../../components/RoleModal.vue";
import DeleteRoleConfirmation from "../../components/DeleteRoleConfirmation.vue";
import { errorToMessage } from "../../../../tailwind-components/app/utils/errorToMessage";
import useAdminSettings from "../../composables/useAdminSettings.ts";
import type { CustomRole, RoleModalMode } from "../../interfaces/interfaces.ts";
import { paginateRows, sortRows } from "../../util/adminUtils";

definePageMeta({
  middleware: "admin-only",
});

const ROLES_SCHEMA_ID = "roles";
const DEFAULT_TABLE_SETTINGS: ITableSettings = {
  page: 1,
  pageSize: 50,
  orderby: {
    column: "schemaId",
    direction: "ASC",
  },
};

const tableFeatures = {
  showSearch: true,
  showStickyHeader: true,
};

const tablePermissions = {
  canInsert: true,
  canUpdate: true,
  canDelete: true,
  canModifySelection: true,
  isRowLevel: false,
};

const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "role", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING_ARRAY" },
  { label: "Users", id: "users", columnType: "STRING_ARRAY" },
];

const tableMetadata: ITableMetaData = {
  id: ROLES_SCHEMA_ID,
  schemaId: ROLES_SCHEMA_ID,
  name: ROLES_SCHEMA_ID,
  label: "Roles",
  tableType: "DATA",
  columns: COLUMNS,
};

const adminSettings = await useAdminSettings();

await adminSettings.getCustomRoles();

const roles = computed<CustomRole[]>(() => adminSettings.customRoles ?? []);
const selectedRows = ref<Map<string, CustomRole>>(new Map());
const tableSettings = ref<ITableSettings>(DEFAULT_TABLE_SETTINGS);
const showCellDetailSideModal = ref(false);
const cellDetailPayload = ref<cellPayload>();
const showRoleModal = ref(false);
const roleModalMode = ref<RoleModalMode>("create");
const roleModalRole = ref<CustomRole>();
const showDeleteRoleModal = ref(false);
const rolesForDeletion = ref<CustomRole[]>([]);
const deleteRolesErrorMessage = ref("");
const deletingRoles = ref(false);

const matchingRoles = computed<CustomRole[]>(() => {
  const search = tableSettings.value.search?.toLowerCase() || "";
  return sortRows(
    filterRoles(search, roles.value),
    tableSettings.value.orderby?.column || "schemaId",
    tableSettings.value.orderby?.direction || "ASC"
  );
});

const rolesShown = computed<CustomRole[]>(() =>
  paginateRows(
    matchingRoles.value,
    tableSettings.value.page,
    tableSettings.value.pageSize
  )
);

function filterRoles(search: string, roles: CustomRole[]): CustomRole[] {
  if (!search) {
    return roles;
  }
  return roles.filter((role) =>
    [role.schemaId, role.role, ...role.tables, ...role.users].some((value) =>
      value?.toLowerCase().includes(search)
    )
  );
}

function openRoleModal(mode: RoleModalMode, role?: CustomRole) {
  roleModalMode.value = mode;
  roleModalRole.value = role;
  showRoleModal.value = true;
}

function showDeleteRoleConfirmation(role?: CustomRole) {
  const candidates = role ? [role] : Array.from(selectedRows.value.values());
  if (!candidates.length) {
    return;
  }
  rolesForDeletion.value = candidates;
  deleteRolesErrorMessage.value = "";
  showDeleteRoleModal.value = true;
}

async function handleDeleteRoles(rolesToDelete: CustomRole[]) {
  deletingRoles.value = true;
  deleteRolesErrorMessage.value = "";
  try {
    await adminSettings.removeCustomRoles(rolesToDelete);
    rolesToDelete.forEach((role) =>
      selectedRows.value.delete(role._rowIdString)
    );
    showDeleteRoleModal.value = false;
  } catch (error) {
    deleteRolesErrorMessage.value = errorToMessage(
      error,
      "Error deleting role(s)"
    );
  } finally {
    deletingRoles.value = false;
  }
}

function handleUpdateSettings(newSettings: ITableSettings) {
  tableSettings.value = newSettings;
}

function handleCellClicked(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showCellDetailSideModal.value = true;
}

function handleToggleRowSelection(row: TableRow) {
  if (selectedRows.value.has(row._rowIdString)) {
    selectedRows.value.delete(row._rowIdString);
  } else {
    selectedRows.value.set(row._rowIdString, rowToRole(row));
  }
}

function handleRowAction(payload: { action: string }) {
  switch (payload.action) {
    case "delete-selection":
      showDeleteRoleConfirmation();
      break;
    case "edit-selection":
      if (selectedRows.value.size === 1) {
        openRoleModal("edit", selectedRows.value.values().next().value);
      }
      break;
    case "select-all-on-page":
      rolesShown.value.forEach((role) => {
        selectedRows.value.set(role._rowIdString, role);
      });
      break;
    case "select-none":
      selectedRows.value.clear();
      break;
  }
}

function rowToRole(row: TableRow): CustomRole {
  return row as CustomRole;
}

function roleDisplayName(row: TableRow): string {
  const role = rowToRole(row);
  return `${role.role} (${role.schemaId})`;
}
</script>

<template>
  <ContentBlock
    class="w-full"
    title="Role management"
    :description="`${matchingRoles.length} roles found`"
  >
    <TableInteractive
      :features="tableFeatures"
      :permissions="tablePermissions"
      :rows="rolesShown"
      :rowCount="matchingRoles.length"
      :selectedRows="selectedRows"
      :settings="tableSettings"
      :tableMetadata="tableMetadata"
      searchPlaceholder="Search roles"
      @update:settings="handleUpdateSettings"
      @cellClicked="handleCellClicked"
      @rowAction="handleRowAction"
      @toggleRowSelection="handleToggleRowSelection"
    >
      <template #row-action="{ row }">
        <Button
          :id="`edit-role-${row._rowIdString}`"
          iconOnly
          type="inline"
          icon="edit"
          :label="`Edit role: ${roleDisplayName(row)}`"
          @click="openRoleModal('edit', rowToRole(row))"
        />
        <Button
          :id="`copy-role-${row._rowIdString}`"
          iconOnly
          type="inline"
          icon="copy"
          :label="`Copy role: ${roleDisplayName(row)}`"
          @click="openRoleModal('copy', rowToRole(row))"
        />
        <Button
          :id="`delete-role-${row._rowIdString}`"
          iconOnly
          type="inline"
          icon="trash"
          :label="`Delete role: ${roleDisplayName(row)}`"
          @click="showDeleteRoleConfirmation(rowToRole(row))"
        />
      </template>

      <template #buttons>
        <Button
          id="create-role"
          type="primary"
          size="medium"
          icon="add-circle"
          @click="openRoleModal('create')"
        >
          Create role
        </Button>
      </template>
    </TableInteractive>

    <CellDetailModal
      v-if="cellDetailPayload"
      :payload="cellDetailPayload"
      :schemaId="ROLES_SCHEMA_ID"
      v-model:showModal="showCellDetailSideModal"
    />

    <RoleModal
      v-if="showRoleModal"
      v-model:visible="showRoleModal"
      :mode="roleModalMode"
      :role="roleModalRole"
    />

    <DeleteRoleConfirmation
      v-if="showDeleteRoleModal"
      v-model:visible="showDeleteRoleModal"
      :roles="rolesForDeletion"
      :errorMessage="deleteRolesErrorMessage"
      :deleting="deletingRoles"
      @deleteRoles="handleDeleteRoles"
    />
  </ContentBlock>
</template>
