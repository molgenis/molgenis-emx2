<script lang="ts" setup>
import { useFetch } from "#app/composables/fetch";
import { useRoute } from "vue-router";
import { useHead, useState } from "#app";
import { parse } from "yaml";
import Container from "../../../../../tailwind-components/app/components/Container.vue";
import ContentBasic from "../../../../../tailwind-components/app/components/content/ContentBasic.vue";
import LoadingContent from "../../../../../tailwind-components/app/components/LoadingContent.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import CustomTooltip from "../../../../../tailwind-components/app/components/CustomTooltip.vue";
import IconProcess from "../../../../../tailwind-components/app/components/icon/Process.vue";
import Table from "../../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../../../tailwind-components/app/components/TableCell.vue";
import type { ProcessData } from "../../../../../metadata-utils/src/generic";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import {
  downloadShacl,
  getProcessData,
  runShacl,
} from "../../../util/shaclUtils";
import { isSuccess } from "../../../util/processUtils";

const route = useRoute();
const routeSchema = (
  Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema
) as string;

useHead({ title: `SHACL - ${routeSchema}  - Molgenis` });

const crumbs: Record<string, string> = {};
crumbs[routeSchema] = `/${routeSchema}`;
crumbs["shacl"] = `/${routeSchema}/shacl`;

const { data, status, error } = await useFetch(`/api/rdf?shacls`, {
  key: "shaclSets",
  getCachedData(key, nuxtApp) {
    return nuxtApp.payload.data[key] || nuxtApp.static.data[key];
  },
  onResponse({ request, response, options }) {
    if (!response._data) {
      throw new Error("Retrieved SHACL set data is empty.");
    }
    response._data = parse(response._data);
  },
  onResponseError() {
    throw new Error(
      "Could not load available SHACL sets. Please check if you have access to any schema's to validate."
    );
  },
});

// Structure: routeSchema -> routeShaclSet -> ProcessData
const shaclSetRuns = useState(
  "shaclSetRuns",
  () => ({} as Record<string, Record<string, ProcessData>>)
);
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
        :status="status"
        loadingText="Loading SHACL sets..."
        :errorText="error?.message"
      >
        <Table>
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
            <TableRow class="group" v-for="shaclSet in data">
              <TableCell>
                <IconProcess
                  :status="shaclSetRuns[routeSchema]?.[shaclSet.id]?.status"
                />
                <!-- todo: replace code below with generic non-EMX2 data table once available -->
              </TableCell>
              <TableCell>
                <div class="flex flex-row">
                  <Button
                    :icon-only="true"
                    type="inline"
                    icon="playArrow"
                    size="small"
                    label="run"
                    @click.prevent="
                      runShacl(
                        getProcessData(routeSchema, shaclSet.id),
                        routeSchema,
                        shaclSet.id
                      )
                    "
                    :disabled="
                      shaclSetRuns[routeSchema]?.[shaclSet.id]?.status ===
                      'RUNNING'
                    "
                  />
                  <NuxtLink :to="`/${routeSchema}/shacl/${shaclSet.id}`">
                    <Button
                      :icon-only="true"
                      type="inline"
                      icon="inspect"
                      size="small"
                      label="inspect"
                    />
                  </NuxtLink>
                  <Button
                    :icon-only="true"
                    type="inline"
                    icon="download"
                    size="small"
                    label="download"
                    @click.prevent="
                      downloadShacl(
                        shaclSetRuns[routeSchema]?.[shaclSet.id],
                        routeSchema,
                        shaclSet.id
                      )
                    "
                    :disabled="
                      !isSuccess(
                        shaclSetRuns[routeSchema]?.[shaclSet.id]?.status
                      )
                    "
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
            </TableRow>
          </template>
        </Table>
      </LoadingContent>
    </ContentBasic>
  </Container>
</template>
