<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <Button icon="plus" @click="createUserModal?.show()">
        Create User
      </Button>

      <h2>User List ({{ userCount }})</h2>
      <Pagination
        v-if="userCount > 100"
        :currentPage="currentPage"
        :totalPages="totalPages"
        @update="updateCurrentPage"
      />
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>Control</TableHead>
            <TableHead>Name / Email</TableHead>
            <TableHead>Enabled</TableHead>
            <TableHead>Roles</TableHead>
            <TableHead>Tokens</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <!-- Why is the first cell blue/link like? -->
          <TableRow v-for="user in users">
            <TableCell>
              <Button
                v-if="canDelete(user)"
                size="tiny"
                icon="trash"
                @click="removeUser(user)"
              />
              <Button size="tiny" icon="user" @click="editUser(user)" />
            </TableCell>
            <TableCell>{{ user.email }}</TableCell>
            <TableCell>{{ user.enabled }}</TableCell>
            <TableCell>
              <div v-for="role in user.roles">
                {{ role.schemaId }} ({{ role.role }})
              </div>
            </TableCell>
            <TableCell> Tokens: {{ user.tokens?.length }} </TableCell>
          </TableRow>
        </template>
      </Table>

      <NewUserModal
        ref="createUserModal"
        :usernames="usernames"
        @addUser="addUser"
      />

      <EditUserModal
        ref="editUserModal"
        :schemas="schemas"
        :roles="roles"
        @userUpdated="retrieveUsers"
      />
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import type { EditUserModal, NewUserModal } from "#build/components";
import {
  createUser,
  deleteUser,
  getRoles,
  getSchemas,
  getUsers,
  type ISchemaInfo,
  type IUser,
} from "~/util/adminUtils";

/**
 * Component wish list:
 *  Settings icon
 *  Edit icon
 *  Admin icon
 *  Buttongroup
 *  Disabled buttons are not greyed out
 *  Icon button with tooltip
 *  Wider modal
 *  Modal without background scrolling
 *  Modal where I can set the close function as a prop
 *  Generic table components
 *  Password input
 *  Select with more complex objects
 *  RadioGroup with more complex objects
 */

/**
 *  Todos (might be other stories)
 *  Do certain actions need confirmation dialog? -> yes
 *  Does password creating/changing need a double input? -> yes
 *  Search bar for users (scale: 100s of users)
 */

/**
 * Todo list
 * make buttons double click safe
 * put component in right place
 * make stuff look better
 */

definePageMeta({
  middleware: "admin-only",
});

const LIMIT = 100;
const editUserModal = ref<InstanceType<typeof EditUserModal>>();
const createUserModal = ref<InstanceType<typeof NewUserModal>>();

const currentPage = ref(1);
const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
const schemas = ref<ISchemaInfo[]>([]);
const roles = ref<string[]>([]);
const schema = ref<string>("");

const offset = computed(() => {
  // todo: use offset
  return currentPage.value > 1
    ? `, offset: ${(currentPage.value - 1) * LIMIT}`
    : "";
});

retrieveUsers();
schemas.value = await getSchemas();
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
  retrieveUsers();
}

function editUser(user: IUser) {
  editUserModal.value?.show(user);
}

function canDelete(user: IUser) {
  return user.email !== "anonymous" && user.email !== "admin";
}
</script>
