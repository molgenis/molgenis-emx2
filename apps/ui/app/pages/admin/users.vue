<template>
  <Container class="flex flex-col items-center">
    <ContentBlock
      class="w-full mt-3"
      title="User management"
      :description="`${adminSettings.userCount} users found`"
    >
      <Button
        icon="plus"
        con="user"
        type="secondary"
        size="small"
        @click="showNewUserModal = true"
      >
        Create User
      </Button>
      <TableInteractive
        :features="tableFeatures"
        :permissions="tablePermissions"
        :rows="adminSettings.users"
        :rowCount="adminSettings.userCount"
        :settings="tableSettings"
        searchPlaceholder="Search users"
        :tableMetadata="tableMetadata"
      >
      </TableInteractive>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Enabled</TableHead>
            <TableHead>Name / Email</TableHead>
            <TableHead>Roles</TableHead>
            <!-- <TableHead>Tokens</TableHead> -->
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="user in adminSettings.users">
            <TableCell>
              <div class="flex gap-1">
                <Button
                  iconOnly
                  icon="user"
                  type="secondary"
                  size="small"
                  :label="`Edit user: ${user.email}`"
                  @click="openEditUserModal(user)"
                />
                <Button
                  v-if="canDelete(user)"
                  iconOnly
                  class="hover:text-red-500"
                  icon="trash"
                  type="secondary"
                  size="small"
                  :label="`Delete user: ${user.email}`"
                  @click="showDeleteUserConfirmation(user)"
                />
              </div>
            </TableCell>
            <TableCell>
              <BaseIcon
                v-if="user.enabled"
                class="text-green-800"
                name="Check"
              />
              <BaseIcon v-else class="text-red-500" name="Cross" />
            </TableCell>
            <TableCell>{{ user.email }}</TableCell>
            <TableCell>
              <div v-if="user.roles && user.roles.length > 3">
                <ShowMore :maxLines="3">
                  <template #more>
                    show all {{ user.roles.length }} roles
                  </template>
                  <div v-for="role in user.roles">
                    {{ role.schemaId }} ({{ role.role }})
                  </div>
                </ShowMore>
              </div>
              <div v-else v-for="role in user.roles">
                {{ role.schemaId }} ({{ role.role }})
              </div>
            </TableCell>
            <TableCell>
              <div
                v-if="user.tokens && user.tokens.length > 0"
                class="flex gap-1"
              >
                <Button
                  type="secondary"
                  size="tiny"
                  @click="openManageTokensModal(user)"
                >
                  {{ user.tokens?.length }} tokens
                </Button>
              </div>
            </TableCell>
          </TableRow>
        </template>
      </Table>
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
        v-if="selectedUser && showDeleteUserModal"
        v-model:visible="showDeleteUserModal"
        :user="selectedUser"
        @deleteUser="adminSettings.removeUser(selectedUser)"
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

<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import ShowMore from "../../../../tailwind-components/app/components/ShowMore.vue";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import type { ITableSettings } from "../../../../tailwind-components/types/types.ts";
import DeleteUserConfirmation from "../../components/DeleteUserConfirmation.vue";
import EditUserModal from "../../components/EditUserModal.vue";
import NewUserModal from "../../components/NewUserModal.vue";
import TokenManagement from "../../components/TokenManagement.vue";
import useAdminSettings from "../../composables/useAdminSettings.ts";
import type { SchemaInfo, User } from "../../interfaces/interfaces.ts";
import { getRoles, getSchemas } from "../../util/adminUtils";
import type { ITableMetaData } from "../../../../metadata-utils/src/types.ts";

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
      columnType: "STRING",
      id: "roles",
      name: "roles",
      label: "Roles",
    },
    {
      columnType: "STRING",
      id: "tokens",
      name: "tokens",
      label: "Tokens",
    },
  ],
};

const showEditUserModal = ref(false);
const showNewUserModal = ref(false);
const showTokenModal = ref(false);
const showDeleteUserModal = ref(false);
const selectedUser = ref<User | null>(null);

const currentPage = ref(1);
const schemas = ref<SchemaInfo[]>([]);
const roles = ref<string[]>([]);
const schema = ref<string>("");

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

async function updateCurrentPage(newPage: number) {
  currentPage.value = newPage;
  await adminSettings.retrieveUsers(newPage);
}

function openEditUserModal(user: User) {
  selectedUser.value = user;
  showEditUserModal.value = true;
}

function showDeleteUserConfirmation(user: User) {
  selectedUser.value = user;
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
</script>
