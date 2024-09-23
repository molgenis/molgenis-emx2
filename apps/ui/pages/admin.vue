<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <h2>User List</h2>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>name</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="user in users">
            <TableCell>{{ user.userName }}</TableCell>
            <TableCell>{{ user }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>

<script lang="ts">
import { request } from "graphql-request";
definePageMeta({
  middleware: "admin-only",
});

const users = ref<Record<string, any>[]>([]);
const userCount = ref(0);

getUsers();

async function getUsers() {
  const result: any = await request(
    "graphql",
    `{_admin{users{email},userCount}}`
  ).catch((error) => {
    console.log("Error loading users: ", error);
  });
  users.value = result?._admin?.users || [];
  userCount.value = result?._admin?.userCount ?? 0;
}
</script>
