<script setup lang="ts">
import { useAsyncData } from "#app";
import { useRoute } from "vue-router";
import fetchGraphql from "../../../../composables/fetchGraphql";
import fetchMetadata from "../../../../composables/fetchMetadata";
import BreadCrumbs from "../../../../components/BreadCrumbs.vue";
import DisplayRecord from "../../../../components/display/Record.vue";
import Message from "../../../../components/Message.vue";
import { deriveRecordQuery } from "../../../../utils/deriveRecordQuery";
import { resolveRecordPath } from "../../../../utils/recordPath";
import { resolveTableConfig } from "../../../../utils/tableConfig";
import { tableConfigs } from "../../../../demoTableConfigs";

const route = useRoute();
const schemaId = String(route.params.schemaId);
const basePath = `/display/record/${schemaId}`;
const segments = [route.params.path ?? []].flat();

const { data, error } = await useAsyncData(
  `record-${schemaId}-${segments.join("/")}`,
  async () => {
    const schema = await fetchMetadata(schemaId);
    const path = resolveRecordPath(schema, tableConfigs, basePath, segments);
    const table = schema.tables.find(
      (candidate) => candidate.id === path.tableId
    );
    if (!table) {
      throw new Error(`Table ${path.tableId} not in schema ${schemaId}`);
    }
    const config = resolveTableConfig(tableConfigs, table);
    const response = await fetchGraphql(
      schemaId,
      deriveRecordQuery(schema, path.tableId, config),
      { filter: path.filter }
    );
    return {
      schema,
      columns: table.columns,
      row: response[path.tableId][0] ?? null,
      config,
      crumbs: path.crumbs,
    };
  }
);
</script>

<template>
  <Message v-if="error" id="record-error" invalid>
    Could not load {{ segments.join("/") }}.
  </Message>
  <Message v-else-if="!data?.row" id="record-missing">
    No record at {{ segments.join("/") }}.
  </Message>
  <DisplayRecord
    v-else
    :columns="data.columns"
    :row="data.row"
    :config="data.config"
    :schema="data.schema"
    :recordPath="data.crumbs[data.crumbs.length - 1]?.url"
  >
    <template #header>
      <BreadCrumbs :crumbs="data.crumbs" align="left" class="mb-7.5" />
    </template>
  </DisplayRecord>
</template>
