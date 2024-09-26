<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <h2>User List ({{ userCount }})</h2>
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
            <TableCell>Edit/Delete/Disable buttons</TableCell>
            <!-- clickable modal for role management? What to show here, count/summary/all? -->
            <TableCell>{{ user.email }}</TableCell>
            <TableCell>{{ user }}</TableCell>
            <TableCell>
              {{ user.settings.length ? user.settings[0].value : "No tokens" }}
            </TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import type { ISetting } from "../../metadata-utils/src/types";

definePageMeta({
  middleware: "admin-only",
});

const LIMIT = 5;
const currentPage = ref(1);
const offset = computed(() => {
  return currentPage.value > 1 ? `, offset: ${currentPage.value * LIMIT}` : "";
});

const users = ref<IUser[]>([]);
const userCount = ref(0);
const totalPages = ref(0);
getUsers();

function updateCurrentPage(newPage: number) {
  currentPage.value = newPage;
  getUsers();
}

async function getUsers() {
  const { data, error } = await useFetch<IAdminResponse>("/api/graphql", {
    method: "post",
    body: {
      query: `{ _admin { users(limit: ${LIMIT}${offset.value}) { email, settings, {key, value} } userCount } }`,
    },
  });
  if (error.value) {
    console.log("Error loading users: ", error.value);
    // todo handle error see catalogue error page
  }
  users.value = data.value?.data._admin.users || [];
  userCount.value = data.value?.data._admin.userCount ?? 0;
  const divided = userCount.value / LIMIT;
  totalPages.value =
    userCount.value % LIMIT > 0 ? Math.floor(divided) + 1 : divided;
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
  email: string;
  settings: ISetting[];
}
</script>
