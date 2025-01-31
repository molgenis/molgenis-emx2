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
    query: `{_schema{id,label,tables{id,label,tableType,description}}}`,
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
  <Container>
    <PageHeader :title="`Tables in ${data?.data._schema.label}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :current="data?.data._schema.label" />
      </template>
    </PageHeader>

    <ContentBlock class="mt-1" title="data tables" description="description">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>label</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="table in tables"
            @click="navigateTo(`${schema}/${table.id}`)"
          >
            <TableCell>{{ table.label }}</TableCell>
            <TableCell>{{ table.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>

    <ContentBlock
      v-if="ontologies.length"
      class="mt-1"
      title="ontolgies"
      description="description"
    >
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>label</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="ontology in ontologies">
            <TableCell>{{ ontology.label }}</TableCell>
            <TableCell>{{ ontology.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
