<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type {
  ITableMetaData,
  TableRow as TableRowData,
} from "../../../../metadata-utils/src/types.ts";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import CellDetailModal from "../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import type {
  cellPayload,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import DeleteUserConfirmation from "../../components/DeleteUserConfirmation.vue";
import EditUserModal from "../../components/EditUserModal.vue";
import NewUserModal from "../../components/NewUserModal.vue";
import TokenManagement from "../../components/TokenManagement.vue";
import useAdminSettings from "../../composables/useAdminSettings.ts";
import type { SchemaInfo, User } from "../../interfaces/interfaces.ts";
import { getRoles, getSchemas } from "../../util/adminUtils";

/**
 * Todo:
 *
 * Add confirmation dialog to delete user / updating of passwords
 * Add search bar for users
 * Make buttons double click safe
 * Implement pagination
 */

definePageMeta({
  middleware: "admin-only",
});

const USER_SCHEMA_ID = "users";

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

const tableMetadata: ITableMetaData = {
  id: "users",
  schemaId: "users",
  name: "users",
  label: "Users",
  tableType: "DATA",
  columns: [
    {
      columnType: "BOOL",
      id: "enabled",
      name: "enabled",
      label: "Enabled",
    },
    {
      columnType: "STRING",
      id: "email",
      name: "email",
      label: "Name / Email",
    },
    {
      columnType: "STRING_ARRAY",
      id: "roleLabels",
      name: "roleLabels",
      label: "Roles",
    },
    {
      columnType: "STRING_ARRAY",
      id: "tokens",
      name: "tokens",
      label: "Tokens",
    },
  ],
};

const showCellInfoSideModal = ref(false);
const cellDetailPayload = ref<cellPayload>();
const showEditUserModal = ref(false);
const showNewUserModal = ref(false);
const showTokenModal = ref(false);
const showDeleteUserModal = ref(false);
const selectedUser = ref<User | null>(null);

const schemas = ref<SchemaInfo[]>([]);
const roles = ref<string[]>([]);
const schema = ref<string>("");
const selectedRows = ref<Map<string, User>>(new Map());
const usersForDeletion = ref<User[]>([]);

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 50,
  orderby: {
    column: "email",
    direction: "ASC",
  },
  search: "",
});
const adminSettings = useAdminSettings();

schemas.value = await getSchemas();
schema.value = schemas.value[0]?.id || "";
roles.value = await getRoles(schemas.value);

const usernames = computed(() => {
  return adminSettings.users.map((user: User) => user.email);
});

const usersShown = computed<TableRowData[]>(() => {
  const search = tableSettings.value.search?.toLowerCase() || "";
  const filteredUsers = adminSettings.users.filter((user: User) => {
    return (
      user.email.toLowerCase().includes(search) ||
      user.roles?.some((role) =>
        role.schemaId.toLowerCase().includes(search)
      ) ||
      user.roles?.some((role) => role.role.toLowerCase().includes(search))
    );
  });
  const sortedUsers = filteredUsers.sort((a, b) => {
    const column = tableSettings.value.orderby?.column || "email";
    const direction = tableSettings.value.orderby?.direction || "ASC";
    const aValue = a[column as keyof User];
    const bValue = b[column as keyof User];
    if (aValue === undefined || bValue === undefined) {
      return 0;
    }
    if (typeof aValue === "string" && typeof bValue === "string") {
      return direction === "ASC"
        ? aValue.localeCompare(bValue)
        : bValue.localeCompare(aValue);
    }
    return 0;
  });
  const paginatedUsers = sortedUsers.slice(
    (tableSettings.value.page - 1) * tableSettings.value.pageSize,
    tableSettings.value.page * tableSettings.value.pageSize
  );
  return paginatedUsers.map(toUserTableRow);
});

const usersByEmail = computed(
  () => new Map(adminSettings.users.map((user: User) => [user.email, user]))
);

function userOf(row: TableRowData): User {
  return usersByEmail.value.get(row._rowIdString)!;
}

function toUserTableRow(user: User): TableRowData {
  return {
    _rowId: { email: user.email },
    _rowIdString: user.email,
    email: user.email,
    enabled: user.enabled,
    roleLabels: (user.roles ?? []).map(
      (role) => `${role.schemaId} (${role.role})`
    ),
    tokens: user.tokens ?? [],
  };
}

async function updateCurrentPage() {}

function openEditUserModal(user: User) {
  selectedUser.value = user;
  showEditUserModal.value = true;
}

function showDeleteUserConfirmation(user?: User) {
  usersForDeletion.value = user
    ? [user]
    : Array.from(selectedRows.value.values());
  showDeleteUserModal.value = true;
}

function openManageTokensModal(user: User) {
  selectedUser.value = user;
  showTokenModal.value = true;
}

function canDelete(user: User) {
  return (
    user.email !== "anonymous" &&
    user.email !== "admin" &&
    user.email !== "user"
  );
}

function handleUpdateSettings(newSettings: ITableSettings) {
  tableSettings.value = newSettings;
  updateCurrentPage();
}

function handleCellClicked(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showCellInfoSideModal.value = true;
}

function handleToggleRowSelection(row: TableRowData) {
  if (selectedRows.value.has(row._rowIdString)) {
    selectedRows.value.delete(row._rowIdString);
  } else {
    selectedRows.value.set(row._rowIdString, userOf(row));
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.size === 1
        ? usersShown.value.find((user) =>
            selectedRows.value.has(user._rowIdString)
          )
        : null;
    switch (action) {
      case "delete-selection":
        showDeleteUserConfirmation();
        break;
      case "edit-selection":
        if (singleRowSelected) {
          openEditUserModal(userOf(singleRowSelected));
        }
        break;
      case "select-all-on-page":
        usersShown.value.forEach((user) => {
          selectedRows.value.set(user._rowIdString, userOf(user));
        });
        break;
      case "select-none":
        selectedRows.value.clear();
        break;
    }
  }
}
</script>

<template>
  <Container class="flex flex-col items-center">
    <ContentBlock
      class="w-full mt-3"
      title="User management"
      :description="`${adminSettings.userCount} users found`"
    >
      <TableInteractive
        :features="tableFeatures"
        :permissions="tablePermissions"
        :rows="usersShown"
        :rowCount="adminSettings.userCount"
        :settings="tableSettings"
        :tableMetadata="tableMetadata"
        searchPlaceholder="Search users"
        @update:settings="handleUpdateSettings"
        @cellClicked="handleCellClicked"
        @rowAction="handleRowAction"
        @toggleRowSelection="handleToggleRowSelection"
      >
        <template #row-action="{ row }">
          <Button
            :id="`edit-user-${row.email}`"
            iconOnly
            type="inline"
            icon="edit"
            :label="`Edit user: ${row.email}`"
            @click="openEditUserModal(userOf(row))"
          />
          <Button
            v-if="canDelete(userOf(row))"
            :id="`delete-user-${row.email}`"
            iconOnly
            type="inline"
            icon="trash"
            :label="`Delete user: ${row.email}`"
            @click="showDeleteUserConfirmation(userOf(row))"
          />
          <Button
            v-if="userOf(row).tokens?.length"
            :id="`manage-tokens-${row.email}`"
            type="inline"
            size="tiny"
            @click="openManageTokensModal(userOf(row))"
          >
            {{ userOf(row).tokens?.length }} tokens
          </Button>
        </template>

        <template #buttons>
          <Button
            type="primary"
            size="medium"
            icon="add-circle"
            @click="showNewUserModal = true"
          >
            Add user
          </Button>
        </template>
      </TableInteractive>

      <CellDetailModal
        v-if="cellDetailPayload"
        :payload="cellDetailPayload"
        :schemaId="USER_SCHEMA_ID"
        v-model:showModal="showCellInfoSideModal"
      />

      <NewUserModal
        v-if="showNewUserModal"
        v-model:visible="showNewUserModal"
        :usernames="usernames"
        @addUser="adminSettings.addUser"
      />

      <EditUserModal
        v-if="selectedUser && showEditUserModal"
        v-model:visible="showEditUserModal"
        :schemas="schemas"
        :roles="roles"
        :user="selectedUser"
        @userUpdated="adminSettings.retrieveUsers"
      />

      <DeleteUserConfirmation
        v-if="usersForDeletion.length && showDeleteUserModal"
        v-model:visible="showDeleteUserModal"
        :users="usersForDeletion"
        @deleteUser="adminSettings.removeUsers(usersForDeletion)"
      />

      <TokenManagement
        v-if="selectedUser && showTokenModal"
        v-model:visible="showTokenModal"
        :user="selectedUser"
        @userUpdated="adminSettings.retrieveUsers"
      />
    </ContentBlock>
  </Container>
</template>
