<script setup lang="ts">
import { gql } from "graphql-request";
import type { ITableMetaData, ISchemaMetaData, IColumn } from "meta-data-utils";
import type { ISection } from "~~/interfaces/types";
import {
  buildRecordDetailsQueryFields,
  extractExternalSchemas,
  buildFilterFromKeysObject,
  isEmpty,
} from "meta-data-utils";

const config = useRuntimeConfig();
const route = useRoute();
const tableId: string = route.params.resourceType as string;
const schemaId = route.params.schema.toString();
const metadata = await fetchMetadata(schemaId);

const tableMetaDataFinderResult = metadata.tables.find(
  (t: ITableMetaData) => t.id.toLowerCase() === tableId.toLowerCase()
);

const tableMetaData = computed(() => {
  if (tableMetaDataFinderResult) {
    return tableMetaDataFinderResult;
  } else {
    throw new Error(`Table metadata not found for ${tableId}`);
  }
});
const resourceType = tableMetaData.value.id;
const schemaIds: string[] = extractExternalSchemas(metadata);

const externalSchemas = await Promise.all(schemaIds.map(fetchMetadata));
const schemas = externalSchemas.reduce(
  (acc: Record<string, ISchemaMetaData>, schema) => {
    acc[schema.id] = schema;
    return acc;
  },
  { [schemaId]: metadata }
);

const fields = buildRecordDetailsQueryFields(schemas, schemaId, tableId);

const { key } = useQueryParams();

const filter = buildFilterFromKeysObject(key);

const query = gql`
  query ${resourceType}($filter: ${resourceType}Filter) {
    ${resourceType}( filter: $filter  ) {
      ${fields}
    }
  }`;

let resource: any;
let sections: any;
let tocItems: { label: string; id: string }[];

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables: { filter: filter } },
  }
);

console.log(query);

watch(data, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  resource = data?.data?.[resourceType][0];
  sections = buildSections(tableMetaData.value, resource);
  tocItems = buildTOC(sections);
}

// Nest fields (data columns) withing sections (headings)
function buildSections(
  tableMetaData: ITableMetaData,
  data: Record<string, any>
) {
  const comparePosition = (a: IColumn, b: IColumn) =>
    (a.position || 0) - (b.position || 0);
  const isHeading = (meta: IColumn) => meta.columnType === "HEADING";
  const isNonSystemField = (meta: IColumn) => !meta.id.startsWith("mg_");
  const hasFieldValue = (field: any) =>
    field !== null && field !== undefined && field !== "" && !isEmpty(field);

  return tableMetaData.columns
    .filter(isNonSystemField)
    .sort(comparePosition)
    .reduce((accum: ISection[], column: IColumn) => {
      if (isHeading(column)) {
        accum.push({ meta: column, fields: [] });
      } else {
        if (!accum.length) {
          accum.push({ meta: column, fields: [] });
        }
        if (hasFieldValue(data[column.id])) {
          accum.at(-1)?.fields.push({
            meta: { ...column },
            value: data[column.id],
          });
        }
      }
      return accum;
    }, []);
}

function buildTOC(sections: ISection[]) {
  return sections.map((section) => ({
    label: sectionTitle(section),
    id: section.meta.id,
  }));
}

function sectionTitle(section: ISection) {
  return section.meta.description
    ? section.meta.description
    : section.meta.label;
}

let crumbs: Record<string, string> = {
  Home: `/${route.params.schema}/ssr-catalogue`,
  Browse: `/${route.params.schema}/ssr-catalogue/browse`,
};
crumbs[
  resourceType
] = `/${route.params.schema}/ssr-catalogue/browse/${resourceType}`;
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="resource?.name" :description="resource?.label">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" :current="resource.id" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="resource?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks>
        <ContentBlockIntro
          id="intro"
          v-if="
            resource?.logo?.url || resource?.website || resource?.contactEmail
          "
          :image="resource?.logo?.url"
          :link="resource?.website"
          :contact="resource?.contactEmail"
        />

        <ContentBlock
          v-if="resource?.description"
          id="description"
          title="Description"
          :description="resource?.description"
        />

        <ContentBlock
          v-for="section in sections"
          :id="section.meta.id"
          :title="sectionTitle(section)"
        >
          <ContentGenericItemList>
            <ContentGenericItem
              v-for="field in section.fields"
              :field="field"
            ></ContentGenericItem>
          </ContentGenericItemList>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
