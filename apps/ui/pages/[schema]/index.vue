<script setup lang="ts">
const route = useRoute();
const schema = route.params.schema;

type Resp<T> = {
  data: Record<string, T>;
};

type TableType = "DATA" | "ONTOLOGIES";

interface Table {
  id: string;
  label: string;
  tableType: TableType;
  schemaId: string;
  description: string;
}

interface Schema {
  id: string;
  label: string;
  tables: Table[];
}

const { data } = await useFetch<Resp<Schema>>(`/${schema}/graphql`, {
  key: "databases",
  method: "POST",
  body: {
    query: `{_schema{id,label,tables{id,label,tableType,schemaId,description}}}`,
  },
});

const tables = computed(
  () =>
    data.value?.data?._schema.tables
      .filter((t) => t.tableType === "DATA")
      .sort((a, b) => a.label.localeCompare(b.label)) ?? []
);

const ontologies = computed(
  () =>
    data.value?.data?._schema.tables
      .filter((t) => t.tableType === "ONTOLOGIES")
      .sort((a, b) => a.label.localeCompare(b.label)) ?? []
);
</script>
<template>
  <Container class="my-3">
    <DisplayList class="text-white" title="Data tables" :columnCount="3">
      <DisplayListItem v-for="table in tables">
        <NuxtLink :to="`/${table.id}`">{{ table.label }}</NuxtLink>
      </DisplayListItem>
    </DisplayList>
  </Container>

  <Container class="my-3">
    <DisplayList class="text-white" title="Ontology tables" :columnCount="3">
      <DisplayListItem v-for="ontology in ontologies">
        <NuxtLink :to="`/${ontology.id}`">{{ ontology.label }}</NuxtLink>
      </DisplayListItem>
    </DisplayList>
  </Container>
</template>
