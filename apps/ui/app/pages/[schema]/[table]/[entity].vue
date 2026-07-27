<script setup lang="ts">
import { createError, navigateTo, useAsyncData, useHead } from "#app";
import { useRoute } from "#app/composables/router";
import { computed } from "vue";
import RecordDetail from "../../../components/RecordDetail.vue";
import fetchMetadata from "../../../../../tailwind-components/app/composables/fetchMetadata";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import { getTitleText } from "../../../../../tailwind-components/app/utils/displayUtils";
import {
  buildCrumbs,
  buildNestedRecordPath,
  parseRecordPath,
} from "../../../../../tailwind-components/app/utils/recordPath";

const route = useRoute();
const schemaId = route.params.schema as string;
const entityId = route.params.entity as string;

const schema = await fetchMetadata(schemaId);
const levels = parseRecordPath(schema, route.fullPath);

if (!levels) {
  const message = `Could not resolve a record from ${route.fullPath}`;
  console.error(message);
  throw createError({ message, statusCode: 404 });
}

const record = levels[levels.length - 1]!;
const columns =
  schema.tables.find((table) => table.id === record.tableId)?.columns ?? [];

const { data: row, refresh } = await useAsyncData(
  `record-${route.fullPath}`,
  () => fetchRowData(schemaId, record.tableId, record.key)
);

const canonicalPath = row.value
  ? buildNestedRecordPath(schema, record.tableId, row.value)
  : null;

if (canonicalPath && canonicalPath !== route.path) {
  await navigateTo(canonicalPath, { replace: true });
} else if (canonicalPath) {
  useHead({ link: [{ rel: "canonical", href: canonicalPath }] });
}

const title = computed(
  () => (row.value && getTitleText(columns, row.value)) || entityId
);

const crumbs = computed(() => buildCrumbs(schema, levels, [title.value]));
</script>

<template>
  <RecordDetail
    :schema-id="schemaId"
    :table-id="record.tableId"
    :row-id="record.key"
    :title="title"
    :crumbs="crumbs"
    @row-changed="refresh"
  />
</template>
