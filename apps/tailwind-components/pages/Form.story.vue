<script setup lang="ts">
import type {
  columnValue,
  IFieldError,
  ISchemaMetaData,
} from "../../metadata-utils/src/types";
import { useRoute } from "#app/composables/router";
import type { FormFields } from "#components";

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

const {
  data: schemaMeta,
  refresh,
  status,
} = await useAsyncData("form sample", () => fetchMetadata(schemaId.value));

const numberOfRows = ref(0);
const rowIndex = ref<null | number>(null);

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

const tableMeta = computed(() => {
  return schemaMeta.value === null
    ? null
    : schemaMeta.value.tables.find((table) => table.id === tableId.value);
});

const formFields = ref<InstanceType<typeof FormFields>>();

const formValues = ref<Record<string, columnValue>>({});

function onModelUpdate(value: Record<string, columnValue>) {
  formValues.value = value;
}

const errors = ref<Record<string, IFieldError[]>>({});

function onErrors(newErrors: Record<string, IFieldError[]>) {
  errors.value = newErrors;
}

watch(
  () => schemaId.value,
  async () => {
    if (schemaMeta.value) {
      await refresh();
      tableId.value = schemaMeta.value.tables[0].id;
      useRouter().push({
        query: {
          ...useRoute().query,
          schema: schemaId.value,
          table: tableId.value,
        },
      });
    }
  }
);

watch(
  () => tableId.value,
  async () => {
    useRouter().push({
      query: {
        ...useRoute().query,
        schema: schemaId.value,
        table: tableId.value,
      },
    });
    getNumberOfRows();
    rowIndex.value = null;
    formValues.value = {};
  },
  { immediate: true }
);

watch(
  () => rowIndex.value,
  async () => {
    if (rowIndex.value !== null) {
      fetchRow(rowIndex.value - 1);
    }
  }
);
</script>

<template>
  <div class="flex flex-row">
    <div class="2/3 p-8 border-l">
      <!-- {{ formValues }} -->
      <FormFields
        id="forms-story"
        v-if="schemaId && tableMeta && status === 'success'"
        ref="formFields"
        :schemaId="schemaId"
        :metadata="tableMeta"
        v-model="formValues"
        @error="onErrors($event)"
      />
    </div>
    <div class="basis-1/3 ml-2 h-screen">
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

        {{ formValues }}

        <!-- <div class="mt-4 flex flex-row">
          <div v-if="Object.keys(formValues).length" class="basis-1/2">
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
          <div v-if="Object.keys(errors).length" class="basis-1/2">
            <h3 class="text-label">Errors</h3>

            <dl class="flex flex-col">
              <template v-for="(value, key) in errors">
                <dt class="font-bold">{{ key }}:</dt>
                <dd v-if="value.length" class="ml-1">{{ value }}</dd>
              </template>
            </dl>
          </div>
        </div> -->
      </div>
    </div>
  </div>
</template>
