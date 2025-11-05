<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { ref, onMounted } from "vue";
import { parse } from "yaml";
import type {
  ShaclSet,
  ShaclStatus,
  ShaclSetValidation,
} from "../../../../../../metadata-utils/src/rdf";
import type { Resp } from "../../../../../../tailwind-components/types/types";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import ContentBasic from "../../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../../tailwind-components/app/components/LoadingContent.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import BaseIcon from "../../../../../../tailwind-components/app/components/BaseIcon.vue";
import Table from "../../../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../../../../tailwind-components/app/components/TableCell.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import ButtonDownloadBlob from "../../../../../../tailwind-components/app/components/button/DownloadBlob.vue";
import DisplayOutput from "../../../../../../tailwind-components/app/components/display/Output.vue";
import { navigateTo } from "#app/composables/router";

const route = useRoute();
const routeSchema = (
  Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema
) as string;

useHead({ title: `SHACL - RDF - ${routeSchema}  - Molgenis` });

const crumbs: Record<string, string> = {};
crumbs[routeSchema] = `/${routeSchema}`;
crumbs["rdf"] = "`/${routeSchema}/rdf`";
crumbs["shacl"] = "`/${routeSchema}/rdf/shacl`";

const shaclSetValidations = ref<ShaclSetValidation[]>();
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

  const output = data.value as unknown as string;
  return parse(output);
}

onMounted(async () => {
  Promise.resolve(fetchShacls())
    .then((data) => {
      shaclSetValidations.value = data as unknown as ShaclSetValidation[];
      shaclSetValidations.value.forEach((i) => {
        i.status = "UNKNOWN";
        i.output = "";
        i.error = "";
        i.isViewed = false;
      });
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
    <PageHeader :title="`SHACL dashboard for ${routeSchema}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
      <template #description>
        Validate the RDF API output for the complete schema.
        <CustomTooltip
          label=""
          content="Output is deemed valid if nodes adhere to the requirements or those nodes are not present."
        />
      </template>
    </PageHeader>
    <ContentBasic>
      <LoadingContent
        id="shaclSets"
        :isLoading="loading"
        loadingText="Loading SHACL sets..."
        :errorText="error"
      >
        <Table>
          <template #head>
            <TableHeadRow>
              <TableHead>status</TableHead>
              <TableHead>name</TableHead>
              <TableHead>version</TableHead>
              <TableHead>sources</TableHead>
            </TableHeadRow>
          </template>
          <template #body>
            <TableRow v-for="shaclSet in shaclSetValidations">
              <TableCell
                @click="navigateTo(`/${routeSchema}/rdf/shacl/${shaclSet.id}`)"
              >
                <BaseIcon
                  name="progress-activity"
                  class="animate-spin m-auto flex-none"
                  v-if="shaclSet.status === 'RUNNING'"
                />
                <BaseIcon
                  name="check"
                  class="m-auto flex-none"
                  v-else-if="shaclSet.status === 'VALID'"
                />
                <BaseIcon
                  name="cross"
                  class="m-auto flex-none"
                  v-else-if="shaclSet.status === 'INVALID'"
                />
                <BaseIcon
                  name="exclamation"
                  class="m-auto flex-none"
                  v-else-if="shaclSet.status === 'ERROR'"
                />
              </TableCell>
              <TableCell
                @click="navigateTo(`/${routeSchema}/rdf/shacl/${shaclSet.id}`)"
                >{{ shaclSet.name }}</TableCell
              >
              <TableCell
                class="text-right"
                @click="navigateTo(`/${routeSchema}/rdf/shacl/${shaclSet.id}`)"
                >{{ shaclSet.version }}</TableCell
              >
              <TableCell>
                <ol>
                  <li
                    v-for="source in shaclSet.sources"
                    class="mb-2.5 last:mb-0"
                  >
                    <a class="line-clamp-1" :href="source" target="_blank">{{
                      source
                    }}</a>
                  </li>
                </ol>
              </TableCell>
            </TableRow>
          </template>
        </Table>
      </LoadingContent>
    </ContentBasic>
  </Container>
</template>
