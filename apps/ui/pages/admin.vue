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
            <TableHead>Roles</TableHead>
            <TableHead>Tokens</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <!-- Why is the first cell blue/link like? -->
          <TableRow v-for="user in users">
            <TableCell>
              <Button size="tiny" icon="trash" @click="deleteUser(user)" />
              <Button size="tiny" icon="user" @click="updateUser(user)" />
            </TableCell>
            <TableCell>{{ user.email }}</TableCell>
            <TableCell>
              <div v-for="role in user.roles">
                {{ role.schemaId }} ({{ role.role }})
              </div>
            </TableCell>
            <TableCell> Tokens: {{ user.tokens?.length }} </TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import type { ISetting } from "../../metadata-utils/src/types";

/**
 * Component wishlist:
 *  Settings icon
 *  edit icon
 *  buttongroup
 *  icon button with tooltip
 *  wider modal
 */

/**
 *  Questions
 *  Where to put enable/disable; behind edit or button toggle
 *  Do certain actions need confirmation dialog?
 *  Does password creating/changing need a double input?
 *  Do we want a modal for creation with more options or is create into edit fine?
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
const currentPage = ref(1);
const offset = computed(() => {
  return currentPage.value > 1
    ? `, offset: ${(currentPage.value - 1) * LIMIT}`
    : "";
});

const password = ref<string>("");
const userName = ref<string>("");
const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
getUsers();

function updateCurrentPage(newPage: number) {
  currentPage.value = newPage;
  getUsers();
}

async function getUsers() {
  const { data, error } = await useFetch<IAdminResponse>(API_GRAPHQL, {
    method: "post",
    body: {
      query: `{ _admin { users(limit: ${LIMIT}${offset.value}) { email, settings, {key, value}, roles { schemaId, role } } userCount } }`,
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

function updateUser(user: IUser) {
  console.log("update: ", user);
  return; // placeholder
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
  tokens?: string[];
  roles?: { schemaId: string; role: string }[];
}
</script>
