<script setup lang="ts">
import type {
  columnValue,
  ISchemaMetaData,
} from "../../../metadata-utils/src/types";
import { useRoute, useRouter } from "#app/composables/router";
import Button from "../components/Button.vue";
import { useFetch, useAsyncData } from "#app";
import { fetchMetadata, fetchTableData } from "#imports";
import { ref, computed, watch } from "vue";
import useForm from "../composables/useForm";

type Resp<T> = {
  data: Record<string, T[]>;
};

interface Schema {
  id: string;
  label: string;
  description: string;
}

const route = useRoute();
const router = useRouter();

const schemaId = (route.query.schema as string) ?? "pet store";

const { data: schemaMeta } = await useAsyncData(schemaId + " form data", () =>
  fetchMetadata(schemaId)
);

const schemaTablesIds = computed(() =>
  (schemaMeta.value as ISchemaMetaData)?.tables.map((table) => table.id)
);

const tableId = (route.query.table as string) ?? schemaTablesIds.value[0];
const metadata = computed(() => {
  const tableMetadata = (schemaMeta.value as ISchemaMetaData)?.tables.find(
    (table) => table.id === tableId
  );
  if (!tableMetadata) {
    throw new Error(`Table ${tableId} not found in schema ${schemaId}`);
  }
  return tableMetadata;
});

const rowIndex = route.query.rowIndex
  ? parseInt(route.query.rowIndex as string)
  : undefined;
const formValues = ref<Record<string, columnValue>>({});
const numberOfRows = ref(0);

const sizeResp = await $fetch(`/${schemaId}/graphql`, {
  method: "POST",
  body: {
    query: `query ${tableId} {
          ${tableId}_agg {
            count
          }
        }`,
  },
});
numberOfRows.value = sizeResp.data[tableId + "_agg"].count;

// Only fetch row data if rowIndex is provided
if (rowIndex !== undefined) {
  const valuesResp = await fetchTableData(schemaId, tableId, {
    limit: 1,
    offset: rowIndex - 1, // adjust for 0-based index
  });

  formValues.value = valuesResp.rows[0] ?? {};
}

const { data: schemas } = await useFetch<Resp<Schema>>("/graphql", {
  key: "schemas",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const schemaIds = computed(() =>
  (schemas.value?.data?._schemas ?? [])
    .sort((a, b) => a.label.localeCompare(b.label))
    .map((s) => s.id)
);

const form = useForm(metadata, formValues);

// reload demo on route change
watch(
  () => route.fullPath,
  async () => {
    console.log("Route changed, reloading form demo");
    router.go(0);
  }
);

const numberOfFieldsWithErrors = computed(
  () =>
    Object.values(form.visibleColumnErrors.value).filter(
      (error) => error.length > 0
    ).length
);
</script>

<template>
  <client-only>
    <div class="flex w-full justify-between">
      <div class="flex-1 grow grid grid-cols-4 gap-1 border-theme">
        <div
          id="forms-story-fields-container"
          class="bg-form p-4"
          :class="form.sections.value.length > 0 ? 'col-span-3' : 'col-span-4'"
        >
          <Form :form="form" :initializeAsInsert="rowIndex === undefined" />
        </div>
      </div>
      <div class="w-full max-w-[33.333%] ml-6 h-screen">
        <h2 class="text-heading-1xl py-2">
          Demo controls, settings and status
        </h2>
        <div class="p-4 border-2 mb-2 flex flex-col gap-4">
          <div class="flex flex-col">
            <label for="table-select" class="text-title font-bold"
              >Schema:
            </label>
            <select
              id="table-select"
              v-model="schemaId"
              class="border border-black"
              @change="
                router.push({
                  query: {
                    schema: schemaId,
                    table: undefined,
                    rowIndex: undefined,
                  },
                })
              "
            >
              <option v-for="schemaId in schemaIds">
                {{ schemaId }}
              </option>
            </select>
          </div>

          <div class="flex flex-col">
            <label for="table-select" class="text-title font-bold"
              >Table:
            </label>
            <select
              id="table-select"
              v-model="tableId"
              class="border border-black"
              @change="
                router.push({
                  query: {
                    schema: schemaId,
                    table: tableId,
                    rowIndex: undefined,
                  },
                })
              "
            >
              <option v-for="tableId in schemaTablesIds">
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
                @change="
                  router.push({
                    query: {
                      schema: schemaId,
                      table: tableId,
                      rowIndex: rowIndex ? rowIndex : undefined,
                    },
                  })
                "
              >
                <option :value="undefined">none</option>
                <option v-for="index in numberOfRows" :value="index">
                  {{ index }}
                </option>
              </select>
              initialize form as
              {{ rowIndex === undefined ? "insert" : "edit" }} mode, current
              rowKey:
              {{
                form.rowKey.value ? JSON.stringify(form.rowKey.value) : "none"
              }}
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
            <div v-if="Object.keys(form.visibleColumnErrors).length">
              <h3 class="text-label">Errors</h3>
              {{ form.visibleColumnErrors }}
              <dl class="flex flex-col">
                <template
                  v-for="(value, key) in form.visibleColumnErrors.value"
                >
                  <dt class="font-bold">{{ key }}:</dt>
                  <dd v-if="value" class="ml-1">{{ value }}</dd>
                </template>
              </dl>
            </div>
          </div>

          <Button
            type="outline"
            @click="form.validateAllColumns"
            class="blue"
            size="small"
            >validate all fields</Button
          >
        </div>
      </div>
    </div>
  </client-only>
</template>
