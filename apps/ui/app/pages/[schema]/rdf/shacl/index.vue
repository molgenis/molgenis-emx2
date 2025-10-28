<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { ref, onMounted } from "vue";
import { parse } from "yaml";
import type { ShaclSet } from "../../../../../../metadata-utils/src/rdf";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../../tailwind-components/app/components/CustomTooltip.vue";
import BaseIcon from "../../../../../../tailwind-components/app/components/BaseIcon.vue";
import Message from "../../../../../../tailwind-components/app/components/Message.vue";
import Table from "../../../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableCell from "../../../../../../tailwind-components/app/components/TableCell.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import ButtonDownloadBlob from "../../../../../../tailwind-components/app/components/button/DownloadBlob.vue";
import DisplayOutput from "../../../../../../tailwind-components/app/components/display/Output.vue";

const route = useRoute();
const schema = (
  Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema
) as string;

useHead({ title: `SHACL - RDF - ${schema}  - Molgenis` });

type Resp<T> = {
  data: Record<string, T>;
};

interface Schema {
  id: string;
  label: string;
}

export type ShaclStatus = "UNKNOWN" | "RUNNING" | "VALID" | "INVALID" | "ERROR";

interface ShaclSetValidation extends ShaclSet {
  status: ShaclStatus;
  output: string;
  error: string;
  isViewed: boolean;
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

const shaclSetValidations = ref<ShaclSetValidation[]>();
const showTable = ref<boolean>(true);
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

function validateShaclOutput(output: string): boolean {
  return output
    .substring(0, 100)
    .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

async function runShacl(shaclSet: ShaclSetValidation) {
  shaclSet.status = "RUNNING";
  shaclSet.output = "";
  shaclSet.error = "";

  const res = await fetch(`/${schema}/api/rdf?validate=${shaclSet.id}`);
  shaclSet.output = await res.text();

  if (res.status !== 200) {
    shaclSet.status = "ERROR";
    shaclSet.error = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(shaclSet.output)) {
    shaclSet.status = "VALID";
  } else {
    shaclSet.status = "INVALID";
  }
}

function toggleShaclOutputView(shaclSet: ShaclSetValidation) {
  if (shaclSet.status === "RUNNING" || shaclSet.status === "UNKNOWN") {
    showTable.value = true;
    shaclSet.isViewed = false;
  } else {
    showTable.value = !showTable.value;
    shaclSet.isViewed = !shaclSet.isViewed;
  }
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
        <Table v-else-if="showTable">
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
            <tr v-for="shaclSet in shaclSetValidations">
              <TableCell>
                <BaseIcon
                  name="progress-activity"
                  class="animate-spin m-auto flex-none"
                  width.number="32"
                  v-if="shaclSet.status === 'RUNNING'"
                />
                <BaseIcon
                  name="check"
                  class="m-auto flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'VALID'"
                />
                <BaseIcon
                  name="cross"
                  class="m-auto flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'INVALID'"
                />
                <BaseIcon
                  name="exclamation"
                  class="m-auto flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'ERROR'"
                />
              </TableCell>
              <TableCell>
                <div class="flex flex-col gap-2.5 md:flex-row md:gap-5">
                  <Button
                    type="primary"
                    size="tiny"
                    :disabled="shaclSet.status === 'RUNNING'"
                    @click.prevent="runShacl(shaclSet)"
                  >
                    validate
                  </Button>
                  <Button
                    type="outline"
                    size="tiny"
                    icon="plus"
                    label="view"
                    :disabled="
                      shaclSet.status === 'RUNNING' ||
                      shaclSet.status === 'UNKNOWN'
                    "
                    @click.prevent="toggleShaclOutputView(shaclSet)"
                  />
                  <ButtonDownloadBlob
                    size="tiny"
                    :disabled="
                      shaclSet.status !== 'VALID' &&
                      shaclSet.status !== 'INVALID'
                    "
                    :data="shaclSet.output"
                    mediaType="text/turtle"
                    :fileName="`${schema} - shacl - ${shaclSet.id}.ttl`"
                  />
                </div>
              </TableCell>
              <TableCell>{{ shaclSet.name }}</TableCell>
              <TableCell class="text-right">{{ shaclSet.version }}</TableCell>
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
            </tr>
          </template>
        </Table>
        <div v-for="shaclSet in shaclSetValidations">
          <div v-if="shaclSet.isViewed">
            <div class="flex flex-col md:flex-row gap-2.5">
              <div class="flex items-center gap-2.5">
                <Button
                  class="flex-none"
                  type="outline"
                  size="small"
                  icon="caretLeft"
                  label="go back"
                  @click.prevent="toggleShaclOutputView(shaclSet)"
                />
                <BaseIcon
                  name="progress-activity"
                  class="animate-spin flex-none"
                  width.number="32"
                  v-if="shaclSet.status === 'RUNNING'"
                />
                <BaseIcon
                  name="check"
                  class="flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'VALID'"
                />
                <BaseIcon
                  name="cross"
                  class="flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'INVALID'"
                />
                <BaseIcon
                  name="exclamation"
                  class="flex-none"
                  width.number="32"
                  v-else-if="shaclSet.status === 'ERROR'"
                />
                <h3 class="uppercase text-heading-4xl font-display">
                  {{ shaclSet.name }} (version: {{ shaclSet.version }})
                </h3>
              </div>
              <div class="flex items-center gap-2.5 my-2.5 ml-auto">
                <Button
                  type="primary"
                  size="small"
                  :disabled="shaclSet.status === 'RUNNING'"
                  @click.prevent="runShacl(shaclSet)"
                >
                  validate
                </Button>
                <ButtonDownloadBlob
                  size="small"
                  :disabled="
                    shaclSet.status !== 'VALID' && shaclSet.status !== 'INVALID'
                  "
                  :data="shaclSet.output"
                  mediaType="text/turtle"
                  :fileName="`${schema} - shacl - ${shaclSet.id}.ttl`"
                />
              </div>
            </div>
            <Message
              :id="`shacl-validation-${shaclSet.id}-error`"
              class="my-8"
              :invalid="true"
              v-if="shaclSet.error"
            >
              <span>{{ shaclSet.error }}</span>
            </Message>
            <DisplayOutput class="px-8 my-8 min-w-full overflow-x-auto">
              <pre>{{ shaclSet.output }}</pre>
            </DisplayOutput>
          </div>
        </div>
      </div>
    </div>
  </Container>
</template>
