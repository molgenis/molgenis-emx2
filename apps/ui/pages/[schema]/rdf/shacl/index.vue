<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute } from "#app/composables/router";
import { useHead } from "#app";
import { ref, onMounted } from "vue";
import { parse } from "yaml";
import type { ShaclSetItem } from "../../../../../metadata-utils/src/rdf";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

useHead({ title: `SHACL - RDF - ${schema}  - Molgenis` });

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
crumbs["shacl"] = "/rdf/shacl";

const shaclSets = ref<ShaclSetItem[]>();
const loading = ref<boolean>(true);
const error = ref<string>("");
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  if (res.status !== 200) {
    const err = await res.text;
    throw new Error(err);
  }

  const yaml = await res.text();
  return parse(yaml);
}

onMounted(async () => {
  Promise.resolve(fetchShacls())
    .then((data) => {
      shaclSets.value = data;
    })
    .catch((err) => {
      error.value = `Could not load available SHACL sets. Please check if you have access to any schema's to validate. ${err}`;
    })
    .finally(() => {
      loading.value = false;
    });
});
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
      <p class="flex justify-start">
        Validate the RDF API output for the complete schema.
        <CustomTooltip
          label=""
          content="Output is deemed valid if nodes adhere to the requirements or those nodes are not present."
        />
      </p>
      <div>
        <div class="h-40 flex item-center justify-center" v-if="loading">
          <div class="text-center">
            <BaseIcon
              name="progress-activity"
              class="animate-spin m-auto"
              :width="32"
            />
            <p class="">Loading SHACL sets...</p>
          </div>
        </div>
        <Message
          id="shacl-sets-error-message"
          :invalid="true"
          v-else="!loading && error"
        >
          <span>{{ error }}</span>
        </Message>
        <RdfShaclSetItem
          v-else
          v-for="shaclSet in shaclSets"
          :shacl-set="shaclSet"
        />
      </div>
    </ContentBlock>
  </Container>
</template>
