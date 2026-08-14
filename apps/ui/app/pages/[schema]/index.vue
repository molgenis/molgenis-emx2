<script setup lang="ts">
import { definePageMeta } from "#imports";
import { navigateTo, useFetch, useHead, useRoute } from "nuxt/app";
import { computed, ref } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types.js";
import BreadCrumbs from "../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import Search from "../../../../tailwind-components/app/components/input/Search.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import { useSession } from "../../../../tailwind-components/app/composables/useSession";
import type { Crumb } from "../../../../tailwind-components/types/types";

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
  schemaId: string;
  description: string;
}

interface Schema {
  id: string;
  label: string;
  tables: ITableMetaData[];
}

const { data } = await useFetch<Resp<Schema>>(`/${schema}/graphql`, {
  key: `fetch-tables-for-${schema}`,
  method: "POST",
  body: {
    query: `{_schema{id,label,tables{id,label,tableType,description}}}`,
  },
});

const tables = computed<ITableMetaData[]>(
  () =>
    data.value?.data?._schema?.tables
      ?.filter((t: ITableMetaData) => t.tableType === "DATA")
      .sort((a: ITableMetaData, b: ITableMetaData) =>
        a.label.localeCompare(b.label)
      ) ?? []
);

const ontologies = computed<ITableMetaData[]>(
  () =>
    data.value?.data?._schema?.tables
      ?.filter((t: ITableMetaData) => t.tableType === "ONTOLOGIES")
      .sort((a: ITableMetaData, b: ITableMetaData) =>
        a.label.localeCompare(b.label)
      ) ?? []
);

const { tablePermissions, getTablePermission } = await useSession(schema);

// no permissions at all means the backend did not supply them; fall back to
// showing every table rather than ghosting the whole list
// empty permissions means the user has no access to any tables, so ghost all but ontologies
function canViewTable(table: ITableMetaData): boolean {
  if (!tablePermissions.value) return true;
  return (
    getTablePermission(table.id)?.canView || table.tableType === "ONTOLOGIES"
  );
}

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
            :disabled="!canViewTable(table)"
            @click="canViewTable(table) && navigateTo(`${schema}/${table.id}`)"
          >
            <TableCell>
              <span :class="{ 'text-disabled': !canViewTable(table) }">
                {{ table.label }}
              </span>
            </TableCell>
            <TableCell>
              <span :class="{ 'text-disabled': !canViewTable(table) }">
                {{ table.description }}
              </span>
            </TableCell>
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
