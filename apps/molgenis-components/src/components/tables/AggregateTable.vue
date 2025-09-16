<template>
  <div>
    <Spinner v-if="loading" class="m-3" />
    <div v-else-if="refColumns.length === 0" class="alert alert-warning">
      Not enough input to create an aggregate table. Need at least 1 column with
      permission that can be used as group by.
    </div>
    <div v-else class="border d-inline-block p-2 bg-white">
      <div class="aggregate-options">
        <table>
          <tbody>
            <tr>
              <td>
                <label
                  class="mx-2 col-form-label form-group mb-0 mr-3"
                  for="aggregate-column-select"
                >
                  Column:
                </label>
              </td>
              <td>
                <InputSelect
                  class="mb-0"
                  id="aggregate-column-select"
                  v-model="selectedColumn"
                  @update:modelValue="fetchData"
                  :options="refColumns"
                  required
                />
              </td>
            </tr>
            <tr>
              <td>
                <label
                  class="mx-2 col-form-label form-group mb-0"
                  for="aggregate-row-select"
                >
                  Row:
                </label>
              </td>
              <td>
                <InputSelect
                  class="mb-2"
                  id="aggregate-row-select"
                  v-model="selectedRow"
                  @update:modelValue="fetchData"
                  :options="refColumns"
                  required
                />
              </td>
            </tr>
            <tr v-if="sumFieldOptions.length">
              <td class="align-top">
                <label
                  class="mx-2 col-form-label form-group mb-0 mr-3 pt-0"
                  for="aggregate-column-select"
                >
                  Aggregate function:
                </label>
              </td>
              <td>
                <InputRadio
                  id="input-radio-2"
                  v-model="aggFunction"
                  :options="['count', 'sum']"
                  :isClearable="false"
                  @update:modelValue="fetchData"
                />
              </td>
            </tr>
            <tr v-if="aggFunction === 'sum'">
              <td class="align-top">
                <label
                  class="mx-2 col-form-label form-group mb-0 mr-3 pt-0"
                  for="aggregate-column-select"
                >
                  Summation field:
                </label>
              </td>
              <td>
                <InputSelect
                  class="mb-2"
                  id="aggregate-row-select"
                  v-model="sumField"
                  @update:modelValue="fetchData"
                  :options="sumFieldOptions"
                  required
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>
      <div v-else-if="noResults" class="alert alert-warning">
        No results found
      </div>

      <TableStickyHeaders
        v-else
        :columns="columns"
        :rows="rows"
        :data="aggregateData"
        class="mb-n3"
      >
        <template #column="columnProps">
          {{ columnProps.value }}
        </template>
        <template #row="rowProps">
          {{ rowProps.value }}
        </template>
        <template #cell="cell">
          <div v-if="!cell.value" class="text-center text-black-50">-</div>
          <div v-else-if="cell.value < minimumValue">﹤{{ minimumValue }}</div>
          <div v-else>{{ cell.value }}</div>
        </template>
      </TableStickyHeaders>
    </div>
  </div>
</template>

<style>
.aggregate-options .float-right {
  display: none;
}
</style>

<script setup lang="ts">
import type { IColumn } from "metadata-utils";
import { computed, ref } from "vue";
import Client from "../../client/client";
import { INewClient } from "../../client/IClient";
import InputRadio from "../forms/InputRadio.vue";
import InputSelect from "../forms/InputSelect.vue";
import IAggregateData from "./IAggregateData";
import TableStickyHeaders from "./TableStickyHeaders.vue";

const AGG_FIELD_TYPES = ["INT", "LONG", "DECIMAL"];

const props = withDefaults(
  defineProps<{
    canView: boolean;
    schemaId: string;
    allColumns: IColumn[];
    tableId: string;
    minimumValue?: number;
    graphqlFilter?: Record<string, any>;
  }>(),
  {
    minimumValue: 1,
    graphqlFilter: () => ({}),
  }
);
const selectedColumn = ref("");
const selectedRow = ref("");
const refColumns = ref<string[]>([]);
const loading = ref(false);
const aggFunction = ref<"sum" | "count">("count");
const sumField = ref<string>(
  props.allColumns.find((col) => AGG_FIELD_TYPES.includes(col.columnType))
    ?.id || ""
);
const rows = ref<string[]>([]);
const columns = ref<string[]>([]);
const aggregateData = ref<IAggregateData>({});
const noResults = ref(false);
const errorMessage = ref<string | undefined>(undefined);
const client = ref<INewClient>(Client.newClient(props.schemaId));

const sumFieldOptions = computed(() => {
  return props.allColumns
    .filter((column: IColumn) => {
      return AGG_FIELD_TYPES.includes(column.columnType);
    })
    .map((column: IColumn) => column.id);
});

const optionsById = computed(() => {
  return props.allColumns.reduce((accum: Record<string, IColumn>, column) => {
    accum[column.id] = column;
    return accum;
  }, {});
});

initialize();

async function fetchData() {
  loading.value = true;
  errorMessage.value = undefined;
  rows.value = [];
  columns.value = [];
  aggregateData.value = {};
  if (aggFunction.value === "sum" && !sumField.value) {
    loading.value = false;
    return;
  }

  const row = optionsById.value[selectedRow.value];
  const column = optionsById.value[selectedColumn.value];
  const rowColumn = await getPrimaryKeyColumn(row);
  const columnColumn = await getPrimaryKeyColumn(column);

  client.value
    .fetchAggregateData(
      props.tableId,
      {
        id: selectedColumn.value,
        column: columnColumn,
      },
      {
        id: selectedRow.value,
        column: rowColumn,
      },
      props.graphqlFilter,
      aggFunction.value === "sum" ? "_sum" : "count",
      sumField.value
    )
    .then((responseData) => {
      if (responseData && responseData[props.tableId + "_groupBy"]) {
        responseData[props.tableId + "_groupBy"].forEach(
          (item: Record<string, any>) => addItem(item)
        );
        noResults.value = !columns.value.length;
      } else {
        noResults.value = true;
      }
      loading.value = false;
    })
    .catch((error) => {
      errorMessage.value = error;
      loading.value = false;
    });
}

async function getPrimaryKeyColumn(column: IColumn) {
  if (column.columnType.startsWith("ONTOLOGY")) {
    return "name";
  }

  if (column.columnType === "REFBACK") {
    const keys = await client.value.getPrimaryKeyFields(
      props.schemaId,
      props.tableId
    );
    return `${column.refBackId} { ${keys[0]} }`;
  }

  const schemaId = column.refSchemaId || props.schemaId;
  const tableId = column.refTableId || props.tableId;
  const keys = await client.value.getPrimaryKeyFields(schemaId, tableId);

  if (keys.length === 1) {
    return keys[0];
  } else {
    console.warn("Composite primary keys not supported in AggregateTable");
    return "name";
  }
}

function addItem(item: Record<string, any>) {
  const row = optionsById.value[selectedRow.value];
  const column = optionsById.value[selectedColumn.value];

  const columnLabel: string = getLabel(column, item);
  const rowLabel: string = getLabel(row, item);

  const aggColValue =
    aggFunction.value === "sum"
      ? item["_sum"][sumField.value]
      : item[aggFunction.value];

  if (!aggregateData.value[rowLabel]) {
    aggregateData.value[rowLabel] = { [columnLabel]: aggColValue };
  } else {
    aggregateData.value[rowLabel][columnLabel] = aggColValue;
  }

  if (!columns.value.includes(columnLabel)) {
    columns.value.push(columnLabel);
  }
  if (!rows.value.includes(rowLabel)) {
    rows.value.push(rowLabel);
  }
}

function getLabel(column: IColumn, item: Record<string, any>) {
  const result = item[column.id];
  if (!result) {
    return "not specified";
  } else if (column.columnType === "REFBACK") {
    return result[column.refBackId!]?.name || "not specified";
  } else {
    return result?.name || "not specified";
  }
}

function initialize() {
  if (props.allColumns.length > 0) {
    refColumns.value = getRefTypeColumns(
      props.allColumns as IColumn[],
      props.canView
    );
  }
  if (refColumns.value?.length) {
    selectedColumn.value = refColumns.value[0];
    selectedRow.value = refColumns.value[1] || refColumns.value[0];
    fetchData();
  }
}

function getRefTypeColumns(columns: IColumn[], canView: boolean): string[] {
  return columns
    .filter((column: IColumn) => {
      return (
        (column.columnType.startsWith("REF") && canView) ||
        column.columnType.startsWith("ONTOLOGY") ||
        (column.columnType === "RADIO" && canView)
      );
    })
    .map((column: IColumn) => column.id);
}
</script>

<docs>
<template>
  <demo-item>
    <label class="mt-5">AggregateTable with canview=false</label>
    <AggregateTable
      tableId="Pet"
      schemaId="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
      :canView="false"
    />
    <label class="mt-5">AggregateTable with canview=true</label>
    <AggregateTable
      tableId="Pet"
      schemaId="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
      :canView="true"
    />
    <label class="mt-5">AggregateTable with filters set</label>
    <AggregateTable
      tableId="Pet"
      schemaId="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
      :graphqlFilter="graphqlFilter"
      :canView="true"
    />

    <label class="mt-5">AggregateTable with filters function select</label>
    <AggregateTable
      tableId="Samples"
      schemaId="FORCE_aggregates"
      :allColumns="sumExampleCols"
      :minimumValue="1"
      :canView="false"
    />
  </demo-item>
</template>

<script>
export default {
  data() {
    return {
      allColumns: [
        {
          id: "name",
          columnType: "STRING",
        },
        {
          id: "category",
          columnType: "RADIO",
        },
        {
          id: "tags",
          columnType: "ONTOLOGY_ARRAY",
        },
        {
          id: "orders",
          columnType: "REFBACK",
          refBackId: "pet",
        }
      ],
      graphqlFilter: { name: { like: ["pooky"] } },
      sumExampleCols: [
        {
          id: "researchCenter",
          columnType: "ONTOLOGY",
        },
        {
          id: "primaryTumor",
          columnType: "ONTOLOGY",
        },
        {
          id: "sampleType",
          columnType: "ONTOLOGY",
        },
        {
          id: "samplingPeriod",
          columnType: "ONTOLOGY",
        },
        {
          id: "sex",
          columnType: "ONTOLOGY",
        },
        {
          id: "n",
          columnType: "INT",
        }]
    };
  },
};
</script>
</docs>
