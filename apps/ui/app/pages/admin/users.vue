<template>
  <Container class="flex flex-col items-center">
    <ContentBlock
      class="w-full mt-3"
      title="User management"
      :description="`${userCount} users found`"
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
          <TableRow v-for="user in users">
            <TableCell>
              <div class="flex gap-1">
                <Button
                  iconOnly
                  icon="user"
                  type="secondary"
                  size="small"
                  :label="`Edit user: ${user.email}`"
                  @click="editUser(user)"
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
              <div v-if="user.roles?.length > 3">
                <ShowMore>
                  <template v-slot:button>
                    <Button type="secondary" size="tiny">
                      show all {{ user.roles?.length }} roles
                    </Button>
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
              <div v-if="user.tokens?.length > 0" class="flex gap-1">
                <Button
                  type="secondary"
                  size="tiny"
                  @click="manageTokens(user)"
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
        @addUser="addUser"
      />

      <EditUserModal
        v-if="selectedUser && showEditUserModal"
        v-model:visible="showEditUserModal"
        :schemas="schemas"
        :roles="roles"
        :user="selectedUser"
        @userUpdated="retrieveUsers"
      />

      <DeleteUserConfirmation
        v-if="selectedUser && showDeleteUserModal"
        v-model:visible="showDeleteUserModal"
        :user="selectedUser"
        @deleteUser="removeUser(selectedUser)"
      />

      <TokenManagement
        v-if="selectedUser && showTokenModal"
        v-model:visible="showTokenModal"
        :user="selectedUser"
        @userUpdated="retrieveUsers"
      />
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import {
  createUser,
  deleteUser,
  getRoles,
  getSchemas,
  getUsers,
  type ISchemaInfo,
  type IUser,
} from "~/util/adminUtils";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import ShowMore from "../../../../tailwind-components/app/components/ShowMore.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import NewUserModal from "../../components/NewUserModal.vue";
import EditUserModal from "../../components/EditUserModal.vue";
import DeleteUserConfirmation from "../../components/DeleteUserConfirmation.vue";
import TokenManagement from "~/components/TokenManagement.vue";

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

const LIMIT = 100;
const showEditUserModal = ref(false);
const showNewUserModal = ref(false);
const showTokenModal = ref(false);
const showDeleteUserModal = ref(false);
const selectedUser = ref<IUser | null>(null);

const currentPage = ref(1);
const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
const schemas = ref<ISchemaInfo[]>([]);
const roles = ref<string[]>([]);
const schema = ref<string>("");

retrieveUsers();
schemas.value = await getSchemas();
// @ts-expect-error
schema.value = schemas.value.length ? schemas.value[0].id : "";
roles.value = await getRoles(schemas.value);

const usernames = computed(() => {
  return users.value.map((user) => user.email);
});

async function addUser(userName: string, password: string) {
  await createUser(userName, password);
  retrieveUsers();
}

function updateCurrentPage(newPage: number) {
  currentPage.value = newPage;
  retrieveUsers();
}

async function retrieveUsers() {
  const { newUsers, newUserCount } = await getUsers();
  users.value = newUsers;
  userCount.value = newUserCount;
  const divided = userCount.value / LIMIT;
  totalPages.value =
    userCount.value % LIMIT > 0 ? Math.floor(divided) + 1 : divided;
}

async function removeUser(user: IUser) {
  await deleteUser(user);
  await retrieveUsers();
}

function editUser(user: IUser) {
  selectedUser.value = user;
  showEditUserModal.value = true;
}

function showDeleteUserConfirmation(user: IUser) {
  selectedUser.value = user;
  showDeleteUserModal.value = true;
}

function manageTokens(user: IUser) {
  selectedUser.value = user;
  showTokenModal.value = true;
}

function canDelete(user: IUser) {
  return (
    user.email !== "anonymous" &&
    user.email !== "admin" &&
    user.email !== "user"
  );
}
</script>
