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
import { useFetch, useAsyncData } from "#app";
import { fetchMetadata, fetchTableMetadata, fetchTableData } from "#imports";
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../metadata-utils/src/types";
import type { Resp, Schema } from "./types/types";

const router = useRouter();

const props = withDefaults(
  defineProps<{
    includeRowSelect?: boolean;
  }>(),
  {
    includeRowSelect: true,
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

const route = useRoute();
schemaId.value = (route.query.schema as string) ?? schemaId.value;
const tableId = ref((route.query.table as string) ?? "Types");
const rowIndex = ref<null | number>(null);
if (route.query.rowIndex) {
  rowIndex.value = parseInt(route.query.rowIndex as string);
}

const numberOfRows = ref(0);

const { data: schemas } = await useFetch<Resp<Schema>>("/graphql", {
  key: "schemas",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const { data: schemaMeta, refresh } = await useAsyncData("form sample", () => {
  return fetchMetadata((route.query.schema as string) || schemaId.value);
});

watch(
  schemaMeta,
  async () => {
    metadata.value = await fetchTableMetadata(schemaId.value, tableId.value);
  },
  { immediate: true }
);

const schemaIds = computed(
  () =>
    schemas.value?.data?._schemas
      .sort((a, b) => a.label.localeCompare(b.label))
      .map((s) => s.id) ?? []
);

const schemaTablesIds = computed(
  () => schemaMeta.value?.tables.map((table) => table.id) ?? []
);

watch(
  () => schemaId.value,
  async () => {
    if (schemaMeta.value) {
      await refresh();
      tableId.value = schemaMeta.value.tables[0].id;
      router.push({
        query: {
          schema: schemaId.value,
        },
      });
    }
  }
);

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

watch(
  () => tableId.value,
  async (newTableId, oldTableId) => {
    if (oldTableId !== newTableId && oldTableId !== undefined) {
      rowIndex.value = null;
    }
    const query: { schema: string; table: string; rowIndex?: number } = {
      schema: schemaId.value,
      table: tableId.value,
    };
    if (rowIndex.value !== null) {
      query.rowIndex = rowIndex.value;
    }

    router.push({ query });
    if (props.includeRowSelect) {
      getNumberOfRows();
    }
    formValues.value = {};
    metadata.value = schemaMeta.value?.tables.find(
      (table) => table.id === newTableId
    );
  }
);

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
    const query: { schema: string; table: string; rowIndex?: number } = {
      schema: schemaId.value,
      table: tableId.value,
    };
    if (rowIndex.value !== null) {
      query.rowIndex = rowIndex.value;
    }

    if (rowIndex.value !== route.query.rowIndex) {
      router.push({ query });
    }

    if (rowIndex.value !== null) {
      await getNumberOfRows();
    }

    formValues.value = {};

    if (rowIndex.value !== null) {
      console.log("fetching row", rowIndex.value);
      await fetchRow(rowIndex.value - 1);
    }
  },
  { immediate: true }
);
</script>
