<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute, navigateTo } from "#app/composables/router";
import { useHead } from "#app";
import { computed } from "vue";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

useHead({ title: `RDF - ${schema}  - Molgenis` });

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
      :title="`RDF dashboard for ${data?.data?._schema?.label}`"
      align="left"
    >
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <ContentBlock class="mt-1" title="RDF" description="">
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
          semantics
        </a>
        field.
      </p>
      <nav class="mt-2">
        <h3 class="uppercase text-heading-2xl font-display">
          Available tools
        </h3>
        <ol class="list-disc ml-8">
          <li>
            <NuxtLink :to="`/${schema}/rdf/shacl`" class="underline">
              SHACL Validation
            </NuxtLink>
          </li>
        </ol>
      </nav>
    </ContentBlock>
  </Container>
</template>
