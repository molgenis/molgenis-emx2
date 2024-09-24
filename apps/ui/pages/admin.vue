<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      {{ users }}
      <h2>User List</h2>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>Name</TableHead>
            <TableHead>Roles</TableHead>
            <TableHead>Tokens</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow>
            <TableCell>admin</TableCell>
            <TableCell>admin.username :)</TableCell>
          </TableRow>
          <TableRow v-for="user in users">
            <TableCell>{{ user.email }}</TableCell>
            <TableCell>{{ user }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import gql from "graphql-tag";

definePageMeta({
  middleware: "admin-only",
});

const users = ref<Record<string, any>[]>([]);
const userCount = ref(0);

getUsers();

async function getUsers() {
  const { data, error } = await useFetch("central/graphql", {
    method: "post",
    body: {
      query: gql`
        {
          _admin {
            users(limit: 20, offset: 0) {
              email
            }
            userCount
          }
        }
      `,
    },
  });
  if (error) {
    console.log("Error loading users: ", error);
  } else {
    console.log("data ", data.value);
    users.value = data.value?._admin?.users || [];
    userCount.value = data.value?._admin?.userCount ?? 0;
  }
}
</script>
