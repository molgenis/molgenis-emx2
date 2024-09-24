<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-full mt-3" title="User management">
      <h2>User List</h2>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>Name / Email</TableHead>
            <TableHead>Roles</TableHead>
            <TableHead>Tokens</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="user in users">
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
definePageMeta({
  middleware: "admin-only",
});

const users = ref<Record<string, any>[]>([]);
const userCount = ref(0);

getUsers();

async function getUsers() {
  const { data, error } = await useAsyncData("admin", async () => {
    console.log("init useSession");

    return await $fetch("/api/graphql", {
      method: "POST",
      body: JSON.stringify({
        query: `{ _admin { users { email, settings, {key, value} } userCount } }`,
      }),
    });
  });
  if (error.value) {
    console.error("Error fetching users", error.value);
  } else {
    users.value = data.value.data._admin.users || [];
    userCount.value = data.value.data._admin.userCount ?? 0;
  }
}
// const { data, error } = await useFetch("api/graphql", {
//   method: "post",
//   body: {
//     query: `{ _admin { users(limit: 20, offset: 0) { email } userCount } }`,
//   },
// });
// if (error) {
//   console.log("Error loading users: ", error);
// } else {
//   console.log("data ", data.value);
//   users.value = data.value?._admin?.users || [];
//   userCount.value =  data.value?._admin?.userCount ?? 0;
// }
</script>
