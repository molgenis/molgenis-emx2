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

<script lang="ts">
import { defineComponent } from "vue";
import TableStickyHeaders from "./TableStickyHeaders.vue";
import IAggregateData from "./IAggregateData";
import Client from "../../client/client";
import InputSelect from "../forms/InputSelect.vue";
import { INewClient, aggFunction } from "../../client/IClient";
import type { IColumn } from "meta-data-utils";
import InputRadio from "../forms/InputRadio.vue";

const AGG_FIELD_TYPES = ["INT", "LONG", "DECIMAL"];

export default defineComponent({
  name: "AggregateTable",
  components: { TableStickyHeaders, InputSelect, InputRadio },
  props: {
    canView: {
      type: Boolean,
      required: true,
    },
    schemaId: {
      type: String,
      required: true,
    },
    allColumns: {
      type: Array,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
    graphqlFilter: {
      type: Object,
      default: {},
    },
  },
  data: function () {
    return {
      selectedColumn: "",
      selectedRow: "",
      refColumns: [] as string[],
      loading: false,
      aggFunction: "count",
      aggField: "",
      rows: [] as string[],
      columns: [] as string[],
      aggregateData: {} as IAggregateData,
      noResults: false,
      errorMessage: undefined,
      client: {} as INewClient,
    };
  },
  computed: {
    aggFieldOptions() {
      return (this.allColumns as IColumn[])
        .filter((column: IColumn) => {
          return AGG_FIELD_TYPES.includes(column.columnType);
        })
        .map((column: IColumn) => column.id);
    },
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.errorMessage = undefined;
      this.rows = [];
      this.columns = [];
      this.aggregateData = {};
      if (this.aggFunction === "sum" && !this.aggField) {
        this.loading = false;
        return;
      }
      const responseData = await this.client
        .fetchAggregateData(
          this.tableId,
          {
            id: this.selectedColumn,
            column: "name",
          },
          {
            id: this.selectedRow,
            column: "name",
          },
          this.graphqlFilter,
          this.aggFunction === "count" ? "count" : "_sum",
          this.aggField
        )
        .catch((error) => {
          this.errorMessage = error;
        });
      if (responseData && responseData[this.tableId + "_groupBy"]) {
        responseData[this.tableId + "_groupBy"].forEach((item: any) =>
          this.addItem(item)
        );
        this.noResults = !this.columns.length;
      } else {
        this.noResults = true;
      }
      this.loading = false;
    },
    addItem(item: any) {
      console.log("add item");
      console.log(item);
      console.log(item[this.selectedColumn]);
      console.log(this.selectedRow);
      const column: string = item[this.selectedColumn]?.name || "not specified";
      const row: string = item[this.selectedRow]?.name || "not specified";

      const aggRespField = this.aggFunction === "count" ? "count" : "_sum";
      const aggColValue =
        this.aggFunction === "count"
          ? item[aggRespField]
          : item[aggRespField][this.aggField];

      if (!this.aggregateData[row]) {
        this.aggregateData[row] = { [column]: aggColValue };
      } else {
        this.aggregateData[row][column] = aggColValue;
      }

      if (!this.columns.includes(column)) {
        this.columns.push(column);
      }
      if (!this.rows.includes(row)) {
        this.rows.push(row);
      }
    },
    initialize() {
      if (this.allColumns.length > 0) {
        this.refColumns = getRefTypeColumns(
          this.allColumns as IColumn[],
          this.canView
        );
      }
      if (this.refColumns?.length > 0) {
        this.selectedColumn = this.refColumns[0];
        this.selectedRow = this.refColumns[1] || this.refColumns[0];
        this.fetchData();
      }
    },
  },
  watch: {
    allColumns() {
      this.initialize();
    },
  },
  mounted() {
    this.client = Client.newClient(this.schemaId);
    this.initialize();
  },
});

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
