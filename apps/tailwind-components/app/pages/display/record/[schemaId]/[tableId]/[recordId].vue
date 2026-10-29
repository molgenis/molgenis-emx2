<script setup lang="ts">
import { useAsyncData } from "#app";
import { useRoute } from "vue-router";
import fetchGraphql from "../../../../../composables/fetchGraphql";
import fetchMetadata from "../../../../../composables/fetchMetadata";
import DisplayRecord from "../../../../../components/display/Record.vue";
import Message from "../../../../../components/Message.vue";
import { deriveRecordQuery } from "../../../../../utils/deriveRecordQuery";
import { recordConfigs } from "../../-recordConfigs";

const route = useRoute();
const schemaId = String(route.params.schemaId);
const tableId = String(route.params.tableId);
const recordId = String(route.params.recordId);

const { data, error } = await useAsyncData(
  `record-${schemaId}-${tableId}-${recordId}`,
  async () => {
    const schema = await fetchMetadata(schemaId);
    const table = schema.tables.find((candidate) => candidate.id === tableId);
    const keyColumn = table?.columns.find((column) => column.key === 1);
    if (!table || !keyColumn) {
      throw new Error(`Table ${tableId} has no key in schema ${schemaId}`);
    }
    const config = recordConfigs[tableId] ?? {};
    const response = await fetchGraphql(
      schemaId,
      deriveRecordQuery(schema, tableId, config),
      { filter: { [keyColumn.id]: { equals: recordId } } }
    );
    return {
      schema,
      columns: table.columns,
      row: response[tableId][0] ?? null,
      config,
    };
  }
);
</script>

<template>
  <Message v-if="error" id="record-error" invalid>
    Could not load {{ tableId }} {{ recordId }}.
  </Message>
  <Message v-else-if="!data?.row" id="record-missing">
    No {{ tableId }} with key {{ recordId }}.
  </Message>
  <DisplayRecord
    v-else
    :columns="data.columns"
    :row="data.row"
    :config="data.config"
    :schema="data.schema"
  />
</template>
