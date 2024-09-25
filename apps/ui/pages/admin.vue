<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <h2>User List ({{ userCount }})</h2>
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

const { data, error } = await useFetch<IAdminResponse>("/api/graphql", {
  method: "post",
  body: {
    query: `{ _admin { users { email, settings, {key, value} } userCount } }`,
  },
});
if (error.value) {
  console.log("Error loading users: ", error.value);
  // todo handle error see catalogue error page
}

const users = computed(() => data.value?.data._admin.users || []);
const userCount = computed(() => data.value?.data._admin.userCount ?? 0);

interface IAdminResponse {
  data: {
    _admin: {
      users: { email: string; settings: ISetting[] }[];
      userCount: number;
    };
  };
}
</script>
