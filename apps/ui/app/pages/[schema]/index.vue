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
import { filterTablesByTypeAndRole } from "../../../../tailwind-components/app/utils/groupTablesByRole";

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

const allTables = computed(() => data.value?.data?._schema?.tables ?? []);

const tables = computed(() =>
  filterTablesByTypeAndRole(allTables.value, "DATA", "MAIN")
);

const detailTables = computed(() =>
  filterTablesByTypeAndRole(allTables.value, "DATA", "DETAIL")
);

const ontologies = computed(() =>
  filterTablesByTypeAndRole(allTables.value, "ONTOLOGIES", "MAIN")
);

const detailOntologies = computed(() =>
  filterTablesByTypeAndRole(allTables.value, "ONTOLOGIES", "DETAIL")
);

const crumbs: Crumb[] = [];
if (schema) {
  crumbs.push({ label: schema, url: `/${schema}` });
}
crumbs.push({ label: "tables", url: "" });

const searchPlaceholder = computed(() =>
  ontologies.value.length || detailOntologies.value.length
    ? "Search tables and ontologies..."
    : "Search tables..."
);

const searchString = ref("");

const bySearch = (item: { label: string }) =>
  !searchString.value ||
  item.label.toLowerCase().includes(searchString.value.toLowerCase());

const filteredTables = computed(() => tables.value.filter(bySearch));
const filteredDetailTables = computed(() =>
  detailTables.value.filter(bySearch)
);
const filteredOntologies = computed(() => ontologies.value.filter(bySearch));
const filteredDetailOntologies = computed(() =>
  detailOntologies.value.filter(bySearch)
);
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

    <ContentBlock v-if="detailTables.length" class="mt-1" title="detail tables">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>label</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="table in filteredDetailTables"
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

    <ContentBlock
      v-if="detailOntologies.length"
      class="mt-1"
      title="detail ontologies"
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
            v-for="ontology in filteredDetailOntologies"
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
