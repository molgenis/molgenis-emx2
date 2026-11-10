<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type {
  ITableMetaData,
  TableRow,
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
import type { User } from "../../interfaces/interfaces.ts";

/**
 * Todo:
 *
 * Make buttons double click safe
 */

definePageMeta({
  middleware: "admin-only",
});

const USER_SCHEMA_ID = "users";
const DEFAULT_TABLE_SETTINGS: ITableSettings = {
  page: 1,
  pageSize: 50,
  orderby: {
    column: "email",
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
      id: "roles",
      name: "roles",
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

const showCellDetailSideModal = ref(false);
const cellDetailPayload = ref<cellPayload>();
const showEditUserModal = ref(false);
const showNewUserModal = ref(false);
const showTokenModal = ref(false);
const showDeleteUserModal = ref(false);
const selectedUser = ref<User | null>(null);

const selectedRows = ref<Map<string, User>>(new Map());
const usersForDeletion = ref<User[]>([]);

const tableSettings = ref<ITableSettings>(DEFAULT_TABLE_SETTINGS);
const adminSettings = await useAdminSettings();

const userNames = computed<string[]>(() => {
  return adminSettings.users.map((user: User) => user.email);
});

const usersShown = computed<User[]>(() => {
  const search = tableSettings.value.search?.toLowerCase() || "";
  const filteredUsers = filterUsers(search, adminSettings.users);
  const sortedUsers = sortUsers(
    filteredUsers,
    tableSettings.value.orderby?.column || "email",
    tableSettings.value.orderby?.direction || "ASC"
  );
  const paginatedUsers = paginateUsers(
    sortedUsers,
    tableSettings.value.page,
    tableSettings.value.pageSize
  );
  return paginatedUsers;
});

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
}

function handleCellClicked(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showCellDetailSideModal.value = true;
}

function handleToggleRowSelection(row: TableRow) {
  if (selectedRows.value.has(row._rowIdString)) {
    selectedRows.value.delete(row._rowIdString);
  } else {
    selectedRows.value.set(row._rowIdString, rowToUser(row));
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
          openEditUserModal(singleRowSelected);
        }
        break;
      case "select-all-on-page":
        usersShown.value.forEach((user) => {
          selectedRows.value.set(user._rowIdString, user);
        });
        break;
      case "select-none":
        selectedRows.value.clear();
        break;
    }
  }
}

function filterUsers(search: string, users: User[]): User[] {
  if (!search) {
    return users;
  } else {
    const lowerSearch = search.toLowerCase();
    return users.filter((user: User) => {
      return (
        user.email?.toLowerCase().includes(lowerSearch) ||
        user.roles?.some((role: string) =>
          role.toLowerCase().includes(lowerSearch)
        )
      );
    });
  }
}

function sortUsers(
  users: User[],
  column: string,
  direction: "ASC" | "DESC"
): User[] {
  return users.sort((a, b) => {
    const aValue = JSON.stringify(a[column as keyof User]);
    const bValue = JSON.stringify(b[column as keyof User]);
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
}

function paginateUsers(users: User[], page: number, pageSize: number): User[] {
  const startIndex = (page - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  return users.slice(startIndex, endIndex);
}

function rowToUser(row: TableRow): User {
  return row as User;
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
        :selectedRows="selectedRows"
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
            @click="openEditUserModal(rowToUser(row))"
          />
          <Button
            v-if="canDelete(rowToUser(row))"
            :id="`delete-user-${row.email}`"
            iconOnly
            type="inline"
            icon="trash"
            :label="`Delete user: ${row.email}`"
            @click="showDeleteUserConfirmation(rowToUser(row))"
          />
          <Button
            v-if="Array.isArray(row.tokens) && row.tokens.length"
            :id="`manage-tokens-${row.email}`"
            type="inline"
            size="tiny"
            @click="openManageTokensModal(rowToUser(row))"
          >
            {{ row.tokens?.length }} tokens
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
        v-model:showModal="showCellDetailSideModal"
      />

      <NewUserModal
        v-if="showNewUserModal"
        v-model:visible="showNewUserModal"
        :usernames="userNames"
        @addUser="adminSettings.addUser"
      />

      <EditUserModal
        v-if="selectedUser && showEditUserModal"
        v-model:visible="showEditUserModal"
        :schemas="adminSettings.schemas"
        :roles="adminSettings.roles"
        :user="selectedUser"
        @userUpdated="adminSettings.reloadAdminData"
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
        @userUpdated="adminSettings.reloadAdminData"
      />
    </ContentBlock>
  </Container>
</template>
