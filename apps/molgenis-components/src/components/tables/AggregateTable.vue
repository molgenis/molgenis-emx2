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
          <tr v-if="aggFieldOptions.length">
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
          <tr v-if="aggFunction === '_sum'">
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
                v-model="aggField"
                @update:modelValue="fetchData"
                :options="aggFieldOptions"
              />
            </td>
          </tr>
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
import { computed, ref, watch } from "vue";
import Client from "../../client/client";
import { INewClient, type AggFunction } from "../../client/IClient";
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
const aggFunction = ref<AggFunction>("count");
const aggField = ref("");
const rows = ref<string[]>([]);
const columns = ref<string[]>([]);
const aggregateData = ref<IAggregateData>({});
const noResults = ref(false);
const errorMessage = ref<string | undefined>(undefined);
const client = ref<INewClient>(Client.newClient(props.schemaId));

const aggFieldOptions = computed(() => {
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

watch(
  () => props.allColumns,
  () => {
    initialize();
  }
);

function fetchData() {
  loading.value = true;
  errorMessage.value = undefined;
  rows.value = [];
  columns.value = [];
  aggregateData.value = {};
  if (aggFunction.value === "_sum" && !aggField.value) {
    loading.value = false;
    return;
  }

  const row = optionsById.value[selectedRow.value];
  const column = optionsById.value[selectedColumn.value];
  const rowColumn =
    row.columnType === "REFBACK" ? `${row.refBackId} { name }` : "name";
  const columnColumn =
    column.columnType === "REFBACK" ? `${column.refBackId} { name }` : "name";

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
      aggFunction.value === "count" ? "count" : "_sum",
      aggField.value
    )
    .then((responseData) => {
      if (responseData && responseData[props.tableId + "_groupBy"]) {
        responseData[props.tableId + "_groupBy"].forEach((item: any) =>
          addItem(item)
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

function addItem(item: any) {
  const column: string = item[selectedColumn.value]?.name || "not specified";
  const row: string = item[selectedRow.value]?.name || "not specified";

  const aggRespField = aggFunction.value === "count" ? "count" : "_sum";
  const aggColValue =
    aggFunction.value === "count"
      ? item[aggRespField]
      : item[aggRespField][aggField.value];

  if (!aggregateData.value[row]) {
    aggregateData.value[row] = { [column]: aggColValue };
  } else {
    aggregateData.value[row][column] = aggColValue;
  }

  if (!columns.value.includes(column)) {
    columns.value.push(column);
  }
  if (!rows.value.includes(row)) {
    rows.value.push(row);
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
        column.columnType.startsWith("ONTOLOGY")
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
          columnType: "REF",
        },
        {
          id: "tags",
          columnType: "ONTOLOGY_ARRAY",
        },
        {
          id: "orders",
          columnType: "REFBACK",
          refBackId: "pet",
        },
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
