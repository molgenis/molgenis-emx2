<script setup lang="ts">
import { useFetch } from "#app/composables/fetch";
import { useRoute, navigateTo } from "#app/composables/router";
import { computed } from "vue";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

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
  key: "tables",
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

const crumbs: Record<string, string> = {};
crumbs[schema] = `/${schema}`;
crumbs["tables"] = "";
</script>
<template>
  <Container>
    <PageHeader :title="`Tables in ${data?.data?._schema?.label}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
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
          <TableRow
            v-for="ontology in ontologies"
            @click="navigateTo(`${schema}/${ontology.id}`)"
          >
            <TableCell>{{ ontology.label }}</TableCell>
            <TableCell>{{ ontology.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
