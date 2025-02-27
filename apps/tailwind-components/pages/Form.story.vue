<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  IFieldError,
  IFormLegendSection,
  ISchemaMetaData,
} from "../../metadata-utils/src/types";
import { useRoute } from "#app/composables/router";
import type { FormFields } from "#components";
import Legend from "~/components/form/Legend.vue";
import { isColumnVisible } from "../../molgenis-components/src/components/forms/formUtils/formUtils";

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
const numberOfRows = ref(0);
const rowIndex = ref<null | number>(null);
const formFields = ref<InstanceType<typeof FormFields>>();
const formValues = ref<Record<string, columnValue>>({});
const errors = ref<Record<string, IFieldError[]>>({});

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

const numberOfFieldsWithErrors = computed(
  () => Object.values(errorMap).filter((value) => value).length
);

const numberOfRequiredFields = computed(() =>
  tableMeta.value
    ? tableMeta.value.columns.filter((column) => column.required).length
    : 0
);

const numberOfRequiredFieldsWithData = computed(() =>
  tableMeta.value
    ? tableMeta.value.columns.filter(
        (column) => column.required && formValues.value[column.id]
      ).length
    : 0
);

const visibleMap = reactive<Record<columnId, boolean>>({});

//initialize visibility for headers
if (tableMeta.value) {
  tableMeta.value.columns
    .filter((column) => column.columnType === "HEADING")
    .forEach((column) => {
      if (tableMeta.value) {
        logger.debug(
          isColumnVisible(column, formValues.value, tableMeta.value)
        );
      }
      visibleMap[column.id] =
        !column.visible ||
        (tableMeta.value &&
          isColumnVisible(column, formValues.value, tableMeta.value))
          ? true
          : false;
      logger.debug(
        "check heading " +
          column.id +
          "=" +
          visibleMap[column.id] +
          " expression " +
          column.visible
      );
    });
}

function checkVisibleExpression(column: IColumn) {
  if (
    !column.visible ||
    isColumnVisible(column, formValues.value, tableMeta.value!)
  ) {
    visibleMap[column.id] = true;
  } else {
    visibleMap[column.id] = false;
  }
  logger.debug(
    "checking visibility of " + column.id + "=" + visibleMap[column.id]
  );
}

function goToSection(headerId: string) {
  //requires all elements before id to have check visibility so we know their sizes
  //todo: loading animation this might take while
  //todo: next to visibility we also need to wait until all have retrieved options or ensure refs have fixed size
  if (!tableMeta.value) return;
  for (let i = 0; i < tableMeta.value.columns.length; i++) {
    const column = tableMeta.value.columns[i];
    if (visibleMap[column.id] === undefined) checkVisibleExpression(column);
    if (column.id === "forms-story") break;
  }
  scrollToElementInside("forms-story" + "-fields-container", headerId);
}

const errorMap = reactive<Record<columnId, string>>({});

const activeChapterId: Ref<string | null> = ref(null);

const chapters = computed(() => {
  return tableMeta.value?.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        label: column.label,
        id: column.id,
        columns: [],
        isActive: column.id === activeChapterId.value,
        errorCount: 0,
      });
    } else {
      if (acc.length === 0) {
        acc.push({
          label: "_top",
          id: "_scroll_to_top",
          columns: [],
          isActive: "_scroll_to_top" === activeChapterId.value,
          errorCount: 0,
        });
      }
      acc[acc.length - 1].columns.push(column);
      if (errorMap[column.id]) acc[acc.length - 1].errorCount++;
    }
    return acc;
  }, [] as (IFormLegendSection & { columns: IColumn[] })[]);
});
</script>

<template>
  <div class="flex flex-row">
    <div class="p-8 border-l grow flex flex-row">
      <Legend
        v-if="chapters"
        :sections="chapters"
        @goToSection="goToSection"
        class="pr-2 mr-4"
      />
      <FormFields
        id="forms-story"
        class="grow"
        v-if="chapters && tableMeta"
        ref="formFields"
        :schemaId="schemaId"
        :metadata="tableMeta"
        :chapters="chapters"
        :visibleMap="visibleMap"
        :errorMap="errorMap"
        :activeChapterId="activeChapterId"
        v-model="formValues"
        @error="onErrors($event)"
      />
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

        <div class="mt-4 flex flex-row">
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
        </div>
      </div>
    </div>
  </div>
</template>
