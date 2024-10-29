<script setup lang="ts">
type Resp<T> = {
  data: Record<string, T[]>;
};

interface Schema {
  id: string;
  label: string;
  description: string;
}

const { data } = await useFetch<Resp<Schema>>("/api/graphql", {
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
    <PageHeader title="SSR Catalogue DEV " />

    <ContentBlock
      class="mt-1"
      title="Schema"
      description="Note this is for dev setup only"
    >
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
            @click="navigateTo(`/${database.id}/ssr-catalogue`)"
          >
            <TableCell>{{ database.label }}</TableCell>
            <TableCell>{{ database.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
