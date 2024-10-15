<script setup lang="ts">
import type { Resp } from "../../tailwind-components/types/types";

interface Schema {
  id: string;
  label: string;
  description: string;
}

const { data } = await useFetch<Resp<Schema>>("/graphql", {
  key: "databases",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const databases = computed(
  () =>
    data.value?.data?._schemas.sort((a, b) => a.label.localeCompare(b.label)) ??
    []
);
</script>
<template>
  <Container>
    <PageHeader title="Databases" />

    <ContentBlock class="mt-1" title="Databases" description="description">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>name</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="database in databases"
            @click="navigateTo(`/${database.id}`)"
          >
            <TableCell>{{ database.label }}</TableCell>
            <TableCell>{{ database.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
