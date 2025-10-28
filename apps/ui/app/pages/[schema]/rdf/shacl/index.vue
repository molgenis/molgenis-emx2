<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { ref, onMounted } from "vue";
import { parse } from "yaml";
import type { ShaclSetArray } from "../../../../../../metadata-utils/src/rdf";
import Container from "../../../../../../tailwind-components/app/components/Container.vue"
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue"
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue"
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue"
import BaseIcon from "../../../../../../tailwind-components/app/components/BaseIcon.vue"
import Message from "../../../../../../tailwind-components/app/components/Message.vue"
import Table from "../../../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../../../tailwind-components/app/components/TableHeadRow.vue";
import RdfShaclTableRow from "../../../../../../tailwind-components/app/components/rdf/ShaclTableRow.vue"

const route = useRoute();
const schema = (Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema) as string;

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
crumbs["rdf"] = `/${schema}/rdf`;
crumbs["shacl"] = "";

const shaclSets = ref<ShaclSetArray>();
const loading = ref<boolean>(true);
const error = ref<string>();

async function fetchShacls(): Promise<string> {
  const { data, error, status } = await useFetch<Resp<string>>(
    `/api/rdf?shacls`
  );

  if (!data.value || error.value || status.value === "error") {
    throw new Error(
      "Could not load available SHACL sets. Please check if you have access to any schema's to validate."
    );
  }

  const output = (data.value as unknown) as string;
  return parse(output);
}

onMounted(async () => {
  Promise.resolve(fetchShacls())
    .then((data) => {
      shaclSets.value = (data as unknown) as ShaclSetArray;
    })
    .catch((err) => {
      error.value = err;
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<template>
  <Container>
    <PageHeader
      :title="`SHACL dashboard for ${data?.data?._schema?.label}`"
      align="left"
    >
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <div>
      <p class="flex justify-start">
        Validate the RDF API output for the complete schema.
        <CustomTooltip
          label=""
          content="Output is deemed valid if nodes adhere to the requirements or those nodes are not present."
        />
      </p>
      <div class="mt-8">
        <div class="h-40 flex item-center justify-center" v-if="loading">
          <div class="text-center">
            <BaseIcon
              name="progress-activity"
              class="animate-spin m-auto"
              :width="32"
            />
            <p>Loading SHACL sets...</p>
          </div>
        </div>
        <Message
          id="shacl-sets-error-message"
          class="my-2"
          :invalid="true"
          v-else-if="error"
        >
          <span>{{ error }}</span>
        </Message>
        <Table v-else>
          <template #head>
            <TableHeadRow>
              <TableHead>status</TableHead>
              <TableHead>controls</TableHead>
              <TableHead>name</TableHead>
              <TableHead>version</TableHead>
              <TableHead>sources</TableHead>
            </TableHeadRow>
          </template>
          <template #body>
            <RdfShaclTableRow
              v-for="shaclSet in shaclSets"
              :shacl-set="shaclSet"
            />
          </template>
        </Table>
      </div>
    </div>
  </Container>
</template>
