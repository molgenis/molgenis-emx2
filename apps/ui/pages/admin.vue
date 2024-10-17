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

      <Modal ref="createUserModal" title="Create User">
        <InputString id="New user name" v-model="userName" />
        <InputString id="New user password" v-model="password" />
        <template #footer>
          <Button @click="addUser(userName, password)">Add user</Button>
          <Button @click="closeCreateUserModal">Close</Button>
        </template>
      </Modal>

      <EditUserModal ref="editUserModal" :schemas="schemas" :roles="roles" />
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import { type Modal } from "#build/components";
import type EditUserModal from "~/components/editUserModal.vue";
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
 * Component wishlist:
 *  Settings icon
 *  Edit icon
 *  Admin icon
 *  Buttongroup
 *  Icon button with tooltip
 *  Wider modal
 *  Generic table components
 *  Password input
 *  Select with more complex objects
 *  RadioGroup with more complex objects
 */

/**
 *  Todos (might be other stories)
 *  Where to put enable/disable; behind edit or button toggle -> doesn't really matter, so modal
 *  Do certain actions need confirmation dialog? -> yes
 *  Don't be able to delete admin/anonymous
 *  Does password creating/changing need a double input? -> yes
 *  Do we want a modal for creation with more options or is create into edit fine? -> modal
 *  Search bar for users (100s of users)
 */

/**
 * Todo list
 * make buttons double click safe
 * put component in right place
 * make stuff look better
 * save user (needs backend)
 */

definePageMeta({
  middleware: "admin-only",
});

const LIMIT = 100;
const editUserModal = ref<InstanceType<typeof EditUserModal>>();
const createUserModal = ref<InstanceType<typeof Modal>>();

const currentPage = ref(1);
const password = ref<string>("");
const userName = ref<string>("");
const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
const schemas = ref<ISchemaInfo[]>([]);
const roles = ref<string[]>([]);
const schema = ref<string>("");

const offset = computed(() => {
  return currentPage.value > 1
    ? `, offset: ${(currentPage.value - 1) * LIMIT}`
    : "";
});

retrieveUsers();
schemas.value = await getSchemas();
schema.value = schemas.value.length ? schemas.value[0].id : "";
roles.value = await getRoles(schemas.value);

async function addUser(userName: string, password: string) {
  await createUser(userName, password);
  closeCreateUserModal();
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
  getUsers();
}

function closeCreateUserModal() {
  createUserModal.value?.close();
  userName.value = "";
  password.value = "";
}

function editUser(user: IUser) {
  editUserModal.value?.show(user);
}

function canDelete(user: IUser) {
  return user.email !== "anonymous" && user.email !== "admin";
}
</script>
