<script setup lang="ts">
import type {
  columnId,
  columnValue,
  ISchemaMetaData,
} from "../../metadata-utils/src/types";
import { useRoute } from "#app/composables/router";
import type { FormFields } from "#components";
import Legend from "~/components/form/Legend.vue";

type Resp<T> = {
  data: Record<string, T[]>;
};

interface Schema {
  id: string;
  label: string;
  description: string;
}

const route = useRoute();
const schemaId = ref((route.query.schema as string) ?? "type test");
const tableId = ref((route.query.table as string) ?? "Types");
const rowIndex = ref<null | number>(null);
if (route.query.rowIndex) {
  rowIndex.value = parseInt(route.query.rowIndex as string);
}

const numberOfRows = ref(0);
const formValues = ref<Record<string, columnValue>>({});

const { data: schemas } = await useFetch<Resp<Schema>>("/graphql", {
  key: "schemas",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const schemaIds = computed(
  () =>
    schemas.value?.data?._schemas
      .sort((a, b) => a.label.localeCompare(b.label))
      .map((s) => s.id) ?? []
);

const { data: schemaMeta, refresh } = await useAsyncData("form sample", () =>
  fetchMetadata(schemaId.value)
);

async function getNumberOfRows() {
  const resp = await $fetch(`/${schemaId.value}/graphql`, {
    method: "POST",
    body: {
      query: `query ${tableId.value} {
          ${tableId.value}_agg {
            count
          }
        }`,
    },
  });
  numberOfRows.value = resp.data[tableId.value + "_agg"].count;
}

async function fetchRow(rowNumber: number) {
  const resp = await fetchTableData(schemaId.value, tableId.value, {
    limit: 1,
    offset: rowNumber,
  });

  formValues.value = resp.rows[0];
}

const schemaTablesIds = computed(() =>
  (schemaMeta.value as ISchemaMetaData)?.tables.map((table) => table.id)
);

const metadata = computed(() => {
  const tableMetadata = (schemaMeta.value as ISchemaMetaData)?.tables.find(
    (table) => table.id === tableId.value
  );
  if (!tableMetadata) {
    throw new Error(
      `Table ${tableId.value} not found in schema ${schemaId.value}`
    );
  }
  return tableMetadata;
});

watch(
  () => schemaId.value,
  async () => {
    if (schemaMeta.value) {
      await refresh();
      tableId.value = schemaMeta.value.tables[0].id;
      useRouter().push({
        query: {
          schema: schemaId.value,
        },
      });
    }
  }
);

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

    useRouter().push({ query });
    getNumberOfRows();
    formValues.value = {};
  },
  { immediate: true }
);

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
    useRouter().push({ query });

    formValues.value = {};

    if (rowIndex.value !== null) {
      fetchRow(rowIndex.value - 1);
    }
  },
  { immediate: true }
);

const numberOfFieldsWithErrors = computed(
  () => Object.values(errorMap.value).filter((error) => error.length > 0).length
);

const numberOfRequiredFields = computed(() =>
  metadata.value !== null
    ? metadata.value?.columns.filter((column) => column.required).length
    : 0
);

const numberOfRequiredFieldsWithData = computed(() =>
  metadata.value !== null
    ? metadata.value?.columns.filter(
        (column) => column.required && formValues.value[column.id]
      ).length
    : 0
);

const activeChapterId = ref<string>("_scroll_to_top");
const errorMap = ref<Record<columnId, string>>({});

const sections = useSections(metadata, activeChapterId, errorMap);
</script>

<template>
  <div class="flex flex-row">
    <div class="p-8 grow flex flex-row">
      <Legend
        v-if="sections?.length"
        :sections="sections"
        @goToSection="
          scrollToElementInside('forms-story-fields-container', $event)
        "
        class="pr-2 mr-4"
      />
      <div
        id="forms-story-fields-container"
        class="grow h-screen overflow-y-scroll border p-10"
      >
        <FormFields
          class="grow"
          :schemaId="schemaId"
          :sections="sections"
          :metadata="metadata"
          v-model:errors="errorMap"
          v-model="formValues"
          @update:active-chapter-id="activeChapterId = $event"
        />
      </div>
    </div>
    <div class="ml-2 h-screen">
      <h2>Demo controls, settings and status</h2>

      <div class="p-4 border-2 mb-2 flex flex-col gap-4">
        <div class="flex flex-col">
          <label for="table-select" class="text-title font-bold"
            >Schema:
          </label>
          <select
            id="table-select"
            v-model="schemaId"
            class="border border-black"
          >
            <option v-for="schemaId in schemaIds" :value="schemaId">
              {{ schemaId }}
            </option>
          </select>
        </div>

        <div class="flex flex-col">
          <label for="table-select" class="text-title font-bold">Table: </label>
          <select
            id="table-select"
            v-model="tableId"
            class="border border-black"
          >
            <option v-for="tableId in schemaTablesIds" :value="tableId">
              {{ tableId }}
            </option>
          </select>
        </div>

        <div>
          This table has {{ numberOfRows }} rows
          <div class="flex flex-col">
            <label for="row-select" class="text-title font-bold"
              >Show row:
            </label>
            <select
              id="row-select"
              v-model="rowIndex"
              class="border border-black"
            >
              <option :value="null">none</option>
              <option v-for="index in numberOfRows" :value="index">
                {{ index }}
              </option>
            </select>
          </div>
        </div>

        <div class="mt-4">
          <div v-if="Object.keys(formValues).length">
            <h3 class="text-label">Values</h3>
            <dl class="flex flex-col">
              <template v-for="(value, key) in formValues">
                <dt class="font-bold">{{ key }}:</dt>
                <dd v-if="value !== null && value !== undefined" class="pl-3">
                  {{ value }}
                </dd>
              </template>
            </dl>
          </div>
          <div>
            <div>number of error: {{ numberOfFieldsWithErrors }}</div>
          </div>
          <div v-if="Object.keys(errorMap).length">
            <h3 class="text-label">Errors</h3>

            <dl class="flex flex-col">
              <template v-for="(value, key) in errorMap">
                <dt class="font-bold">{{ key }}:</dt>
                <dd v-if="value.length" class="ml-1">{{ value }}</dd>
              </template>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
