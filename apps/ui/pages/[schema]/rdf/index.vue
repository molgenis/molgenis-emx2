<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { navigateTo, useRoute } from "#app/composables/router";
import { useHead } from "#app";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

useHead({ title: `RDF - ${schema} - Molgenis` });

type Resp<T> = {
  data: Record<string, T>;
};

interface Schema {
  id: string;
  label: string;
}

const { data } = await useFetch<Resp<Schema>>(`/${schema}/graphql`, {
  key: "tables",
  method: "POST",
  body: {
    query: `{_schema{id,label}}`,
  },
});

const crumbs: Record<string, string> = {};
crumbs[schema] = `/${schema}`;
crumbs["rdf"] = "";
</script>

<template>
  <Container>
    <PageHeader
      :title="`RDF services for ${data?.data?._schema?.label}`"
      align="left"
    >
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <div>
      <Table class="mb-2.5">
        <template #head>
          <TableHeadRow>
            <TableHead>name</TableHead>
            <TableHead>description</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow @click="navigateTo(`/${schema}/rdf/shacl`)">
            <TableCell> SHACL Validation</TableCell>
            <TableCell
              >Validate a schema as a whole to see if it adheres to existing
              standards</TableCell
            >
          </TableRow>
          <TableRow
            v-for="table in tables"
            @click="navigateTo(`${schema}/${table.id}`)"
          >
            <TableCell>{{ table.label }}</TableCell>
            <TableCell>{{ table.description }}</TableCell>
          </TableRow>
        </template>
      </Table>
      <p>
        For information about RDF in EMX2, please view the docs about the
        <a
          href="https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_rdf"
          target="_blank"
          class="underline"
        >
          RDF API
        </a>
        and the
        <a
          href="https://molgenis.github.io/molgenis-emx2/#/molgenis/semantics"
          target="_blank"
          class="underline"
        >
          semantics field</a
        >.
      </p>
    </div>
  </Container>
</template>
