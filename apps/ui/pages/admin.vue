<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <h2>User List ({{ userCount }})</h2>
      <div>
        <InputString id="New user name" v-model="userName" />
        <InputString id="New user password" v-model="password" />
        <Button icon="plus" size="tiny" @click="createUser(userName, password)">
          Add user
        </Button>
      </div>
      <Pagination
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
                v-if="user.email !== 'anonymous' && user.email !== 'admin'"
                size="tiny"
                icon="trash"
                @click="deleteUser(user)"
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
      <Modal ref="editUserModal" title="Edit User">
        {{ selectedUser }}
        <template #footer>
          <Button @click="updateUser(selectedUser)">Save</Button>
          <Button @click="closeEditUserModal">Close</Button>
        </template>
      </Modal>
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import type { Modal } from "#build/components";
import type { ISetting } from "../../metadata-utils/src/types";

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
 */

/**
 *  Todos (might be other stories)
 *  Where to put enable/disable; behind edit or button toggle -> doesn't really matter, so modal
 *  Do certain actions need confirmation dialog? -> yes
 *  Don't be able to delete admin/anonymous
 *  Does password creating/changing need a double input? -> yes
 *  Do we want a modal for creation with more options or is create into edit fine? -> modal
 *  Search bar for users (100s of users)
 *
 */

/**
 * Todo list
 * make edit modal -> need wider modal -> pass data into modal, have onclose?
 * make buttons double click safe
 * put component in right place
 * make stuff look better
 */

definePageMeta({
  middleware: "admin-only",
});

const GRAPHQL = "/graphql";
const API_GRAPHQL = "/api/graphql";
const LIMIT = 20;
const editUserModal = ref<InstanceType<typeof Modal>>();

const currentPage = ref(1);
const password = ref<string>("");
const userName = ref<string>("");
const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
const selectedUser = ref<IUser>();

const offset = computed(() => {
  return currentPage.value > 1
    ? `, offset: ${(currentPage.value - 1) * LIMIT}`
    : "";
});

getUsers();

function updateCurrentPage(newPage: number) {
  currentPage.value = newPage;
  getUsers();
}

async function getUsers() {
  const { data, error } = await useFetch<IAdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query: `{ _admin { users { email, settings, {key, value}, enabled, roles { schemaId, role } } userCount } }`,
    },
  });
  if (error.value) {
    handleError("Error loading users: ", error.value);
    // todo handle error see catalogue error page
  }
  const transformedUsers = buildUsers(data.value?.data._admin.users || []);
  users.value = transformedUsers;
  userCount.value = data.value?.data._admin.userCount ?? 0;
  const divided = userCount.value / LIMIT;
  totalPages.value =
    userCount.value % LIMIT > 0 ? Math.floor(divided) + 1 : divided;
}

function buildUsers(dataUsers: IUser[]) {
  return dataUsers.map((user) => {
    return { ...user, tokens: getTokens(user) };
  });
}

function getTokens(user: IUser) {
  if (user.settings.length) {
    const tokens = user.settings.find((setting) => {
      return setting.key === "access-tokens";
    });
    if (tokens) {
      return tokens.value.split(",");
    }
  }
  return [];
}

function deleteUser(user: IUser) {
  useFetch(API_GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{removeUser(email: "${user.email}"){status,message}}`,
      member: user.email,
    },
  })
    .then(getUsers)
    .catch((error) => {
      handleError("Error deleting user: ", error.value);
    });
}

function updateUser(user?: IUser) {
  if (!user) return;

  const userToSend = user;
  useFetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation change($editMember:MolgenisMembersInput){change(members:[$editMember]){message}}`,
      editMember: userToSend,
    },
  }).catch((error) => {
    handleError("Error updating user: ", error.value);
  });
}

function createUser(newUserName: string, newPassword: string) {
  if (!newUserName || !newPassword) return;

  useFetch(GRAPHQL, {
    method: "post",
    body: {
      query: `mutation{changePassword(email: "${newUserName}", password: "${newPassword}"){status,message}}`,
    },
  })
    .then(() => {
      userName.value = "";
      password.value = "";
    })
    .then(getUsers)
    .catch((error) => {
      handleError("Error creating/updating user: ", error.value);
    });
}

function handleError(message: string, error: any) {
  console.log(message, error);
  //see nuxt catalogue on how to handle errors
}

function editUser(user: IUser) {
  selectedUser.value = JSON.parse(JSON.stringify(user));
  editUserModal.value?.show();
}

function closeEditUserModal() {
  selectedUser.value = undefined;
  editUserModal.value?.close();
}

interface IAdminResponse {
  data: {
    _admin: {
      users: IUser[];
      userCount: number;
    };
  };
}

interface IUser {
  //TODO split into communication and internal interface
  email: string;
  settings: ISetting[];
  enabled: boolean;
  tokens?: string[];
  roles?: { schemaId: string; role: string }[];
}
</script>
