<template>
  <fieldset class="p-4 border-2 mb-2 flex flex-col gap-4">
    <legend class="text-title font-bold">Demo data controls</legend>

    <div class="flex flex-col">
      <label for="table-select" class="text-title font-bold">Schema: </label>
      <select id="table-select" v-model="schemaId" class="border border-black">
        <option v-for="schemaId in schemaIds" :value="schemaId">
          {{ schemaId }}
        </option>
      </select>
    </div>

    <div class="flex flex-col">
      <label for="table-select" class="text-title font-bold">Table: </label>
      <select id="table-select" v-model="tableId" class="border border-black">
        <option v-for="tableId in schemaTablesIds" :value="tableId">
          {{ tableId }}
        </option>
      </select>
    </div>

    <div v-if="includeRowSelect">
      This table has {{ numberOfRows }} rows
      <div class="flex flex-col">
        <label for="row-select" class="text-title font-bold">Show row: </label>
        <select id="row-select" v-model="rowIndex" class="border border-black">
          <option :value="null">none</option>
          <option v-for="index in numberOfRows" :value="index">
            {{ index }}
          </option>
        </select>
      </div>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import { useFetch } from "#app";
import { fetchMetadata, fetchTableMetadata, fetchTableData } from "#imports";
import { ref, watch, computed } from "vue";
import {
  type ISchemaMetaData,
  type columnId,
  type columnValue,
  type ITableMetaData,
} from "../../metadata-utils/src/types";
import type { Resp, Schema } from "../types/types";

const props = withDefaults(
  defineProps<{
    includeRowSelect?: boolean;
    rowIndex?: number;
  }>(),
  {
    includeRowSelect: false,
  }
);

const schemaId = defineModel<string>("schemaId", {
  required: true,
});

const metadata = defineModel<ITableMetaData | undefined>("metadata", {
  required: true,
});

const formValues = defineModel<Record<columnId, columnValue>>("formValues", {
  required: false,
});

const tableId = defineModel<string>("tableId", {
  required: true,
});

const schemaMeta = ref<ISchemaMetaData | null>(null);

if (schemaId.value) {
  schemaMeta.value = await fetchMetadata(schemaId.value);
}

const rowIndex = ref<null | number>(null);

const numberOfRows = ref(0);

const { data: schemas } = await useFetch<Resp<Schema>>("/graphql", {
  key: "schemas",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const schemaIds = computed(
  () =>
    schemas.value?.data?._schemas
      ?.sort((a, b) => a.label.localeCompare(b.label))
      .map((s) => s.id) ?? []
);

const schemaTablesIds = computed(
  () => schemaMeta.value?.tables.map((table: any) => table.id) ?? []
);

if (!metadata.value && schemaId.value && tableId.value) {
  metadata.value = await fetchTableMetadata(schemaId.value, tableId.value);
  if (props.includeRowSelect) {
    await getNumberOfRows();
    if (props.rowIndex !== undefined && props.rowIndex !== null) {
      rowIndex.value = props.rowIndex;
      await fetchRow(rowIndex.value - 1);
    }
  }
}

async function getNumberOfRows() {
  // lock (unref) the tableId insiude closure, to make sure the same id is used in the request as while reading the respqonse
  const countTableId = tableId.value;
  const resp = await $fetch(`/${schemaId.value}/graphql`, {
    method: "POST",
    body: {
      query: `query ${countTableId} {
          ${countTableId}_agg {
            count
          }
        }`,
    },
  });
  numberOfRows.value = resp.data[countTableId + "_agg"].count;
}

async function fetchRow(rowNumber: number) {
  const resp = await fetchTableData(schemaId.value, tableId.value, {
    limit: 1,
    offset: rowNumber,
  });

  formValues.value = resp.rows[0];
}

watch(
  () => rowIndex.value,
  async () => {
    formValues.value = {};

    if (rowIndex.value !== null) {
      await fetchRow(rowIndex.value - 1);
    }
  }
);

watch(
  () => schemaId.value,
  async () => {
    schemaMeta.value = await fetchMetadata(schemaId.value);
    tableId.value = schemaMeta.value?.tables[0]?.id ?? "";
    if (tableId.value) {
      metadata.value = await fetchTableMetadata(schemaId.value, tableId.value);
    }
  }
);

watch(
  () => tableId.value,
  async () => {
    if (schemaId.value && tableId.value) {
      metadata.value = await fetchTableMetadata(schemaId.value, tableId.value);
      if (props.includeRowSelect) {
        await getNumberOfRows();
      }
    }
  }
);
</script>
