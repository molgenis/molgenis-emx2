<script setup lang="ts">
import { useFetch } from "#app/composables/fetch";
import { useRoute, navigateTo } from "#app/composables/router";
import { useHead } from "#app";
import { computed, ref } from "vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import BreadCrumbs from "../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import type { Crumb } from "../../../../tailwind-components/types/types";
import { definePageMeta } from "#imports";
import Search from "../../../../tailwind-components/app/components/input/Search.vue";

definePageMeta({
  middleware: ["landing-page"],
});

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

useHead({ title: `${schema}  - Molgenis` });

type Resp<T> = {
  data: Record<string, T>;
};

type TableType = "DATA" | "ONTOLOGIES";

interface Table {
  id: string;
  label: string;
  tableType: TableType;
  role?: string;
  schemaId: string;
  description: string;
}

interface Schema {
  id: string;
  label: string;
  tables: Table[];
}

const { data } = await useFetch<Resp<Schema>>(`/${schema}/graphql`, {
  key: `fetch-tables-for-${schema}`,
  method: "POST",
  body: {
    query: `{_schema{id,label,tables{id,label,tableType,role,description}}}`,
  },
});

const tables = computed(
  () =>
    data.value?.data?._schema?.tables
      ?.filter((t) => t.tableType === "DATA" && (!t.role || t.role === "MAIN"))
      .sort((a, b) => a.label.localeCompare(b.label)) ?? []
);

const ontologies = computed(
  () =>
    data.value?.data?._schema?.tables
      ?.filter(
        (t) => t.tableType === "ONTOLOGIES" && (!t.role || t.role === "MAIN")
      )
      .sort((a, b) => a.label.localeCompare(b.label)) ?? []
);

const crumbs: Crumb[] = [];
if (schema) {
  crumbs.push({ label: schema, url: `/${schema}` });
}
crumbs.push({ label: "tables", url: "" });

const searchPlaceholder = ontologies.value.length
  ? "Search tables and ontologies..."
  : "Search tables...";

const searchString = ref("");

const filteredTables = computed(() => {
  if (!searchString.value) return tables.value;
  return tables.value.filter((table) =>
    table.label.toLowerCase().includes(searchString.value.toLowerCase())
  );
});

const filteredOntologies = computed(() => {
  if (!searchString.value) return ontologies.value;
  return ontologies.value.filter((ontology) =>
    ontology.label.toLowerCase().includes(searchString.value.toLowerCase())
  );
});
</script>
<template>
  <Container>
    <PageHeader :title="`Tables in ${data?.data?._schema?.label}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>

    <Search
      id="tables-search-input"
      :placeholder="searchPlaceholder"
      v-model="searchString"
      class="mb-4"
    ></Search>

    <ContentBlock class="mt-1" title="data tables">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>label</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="table in filteredTables"
            @click="navigateTo(`${schema}/${table.id}`)"
          >
            <TableCell>{{ table.label }}</TableCell>
            <TableCell>{{ table.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>

    <ContentBlock v-if="ontologies.length" class="mt-1" title="ontologies">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>label</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="ontology in filteredOntologies"
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
