<script setup lang="ts">
import { useFetch } from "#app/composables/fetch";
import { computed, ref } from "vue";
import type { Resp } from "../../tailwind-components/types/types";
import { navigateTo } from "#app/composables/router";

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

function filterDatabases(database: Schema) {
  return (
    filter.value === "" ||
    database.label.toLowerCase().includes(filter.value.toLowerCase()) ||
    database.description?.toLowerCase().includes(filter.value.toLowerCase())
  );
}

const databases = computed(
  () =>
    data.value?.data?._schemas
      .filter(filterDatabases)
      .sort((a, b) => a.label.localeCompare(b.label)) ?? []
);
const handleSearchRequest = (search: string) => {
  filter.value = search;
};
const filter = ref("");
</script>
<template>
  <Container>
    <PageHeader title="Databases" />

    <div class="flex flex-row justify-center items-center mb-4">
      <InputSearch
        v-model="filter"
        placeholder="Search databases"
        id="search-input"
      />
    </div>

    <ContentBlock class="mt-1" title="" description="">
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
