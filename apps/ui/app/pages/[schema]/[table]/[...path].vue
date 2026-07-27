<script setup lang="ts">
import { createError, useAsyncData } from "#app";
import { useRoute } from "#app/composables/router";
import { computed } from "vue";
import type { ISchemaMetaData } from "../../../../../metadata-utils/src/types";
import ContextualRecordList from "../../../components/ContextualRecordList.vue";
import RecordDetail from "../../../components/RecordDetail.vue";
import fetchMetadata from "../../../../../tailwind-components/app/composables/fetchMetadata";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import {
  buildRefbackFilter,
  getTitleText,
} from "../../../../../tailwind-components/app/utils/displayUtils";
import {
  buildCrumbs,
  parseContextualListPath,
  parseRecordPath,
  type RecordPathLevel,
} from "../../../../../tailwind-components/app/utils/recordPath";

const route = useRoute();
const schemaId = route.params.schema as string;

const schema = await fetchMetadata(schemaId);
const recordLevels = parseRecordPath(schema, route.path);
const contextualList = recordLevels
  ? null
  : parseContextualListPath(schema, route.path);

if (!recordLevels && !contextualList) {
  const message = `Could not resolve a record from ${route.path}`;
  console.error(message);
  throw createError({ message, statusCode: 404 });
}

const levels = recordLevels ?? contextualList!.levels;
const record = recordLevels ? recordLevels[recordLevels.length - 1]! : null;

const { data: row, refresh } = await useAsyncData(`record-${route.path}`, () =>
  record
    ? fetchRowData(schemaId, record.tableId, record.key)
    : Promise.resolve(null)
);

const labelledLevels = record ? levels.slice(0, -1) : levels;
const levelLabels = await Promise.all(
  labelledLevels.map((level) => fetchRecordLabel(level))
);

const title = computed(
  () =>
    (record &&
      row.value &&
      getTitleText(columnsOf(schema, record.tableId), row.value)) ||
    record?.keySegment ||
    ""
);

const listTitle = contextualList
  ? tableLabelOf(schema, contextualList.tableId)
  : "";

const parentCrumbs = buildCrumbs(schema, levels, levelLabels);
const parentRecordPath = parentCrumbs[parentCrumbs.length - 1]?.url ?? "";

const crumbs = computed(() =>
  record
    ? buildCrumbs(schema, levels, [...levelLabels, title.value])
    : [...parentCrumbs, { url: route.path, label: listTitle }]
);

const parentLevel = levels[levels.length - 1]!;
const partsColumn = contextualList
  ? columnsOf(schema, parentLevel.tableId).find(
      (column) => column.id === contextualList.partsColumnId
    )
  : undefined;

const listFilter =
  contextualList && partsColumn
    ? buildRefbackFilter(
        partsColumn.columnType,
        contextualList.partOfColumnId,
        parentLevel.key
      )
    : undefined;

async function fetchRecordLabel(
  level: RecordPathLevel
): Promise<string | undefined> {
  try {
    const levelRow = await fetchRowData(schemaId, level.tableId, level.key, 1);
    return (
      getTitleText(columnsOf(schema, level.tableId), levelRow) || undefined
    );
  } catch (error) {
    console.error(
      `Could not label breadcrumb for ${level.tableId} ${level.keySegment}`,
      error
    );
    return undefined;
  }
}

function columnsOf(schemaMetadata: ISchemaMetaData, tableId: string) {
  return (
    schemaMetadata.tables.find((table) => table.id === tableId)?.columns ?? []
  );
}

function tableLabelOf(schemaMetadata: ISchemaMetaData, tableId: string) {
  const table = schemaMetadata.tables.find((entry) => entry.id === tableId);
  return table?.label || tableId;
}
</script>

<template>
  <RecordDetail
    v-if="record"
    :schema-id="schemaId"
    :table-id="record.tableId"
    :row-id="record.key"
    :title="title"
    :crumbs="crumbs"
    @row-changed="refresh"
  />
  <ContextualRecordList
    v-else-if="contextualList"
    :schema-id="schemaId"
    :table-id="contextualList.tableId"
    :filter="listFilter"
    :hide-columns="contextualList.contextColumnIds"
    :title="listTitle"
    :crumbs="crumbs"
    :parent-record-path="parentRecordPath"
  />
</template>
