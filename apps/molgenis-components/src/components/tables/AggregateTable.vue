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
import { IColumn } from "../../Interfaces/IColumn";
import { convertToCamelCase } from "../utils";
import { INewClient } from "../../client/IClient";

export default defineComponent({
  name: "AggregateTable",
  components: { TableStickyHeaders, InputSelect },
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
      rows: [] as string[],
      columns: [] as string[],
      aggregateData: {} as IAggregateData,
      noResults: false,
      errorMessage: undefined,
      client: {} as INewClient,
    };
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.errorMessage = undefined;
      this.rows = [];
      this.columns = [];
      this.aggregateData = {};
      const responseData = await this.client
        .fetchAggregateData(
          this.tableId,
          {
            name: convertToCamelCase(this.selectedColumn),
            column: "name",
          },
          {
            name: convertToCamelCase(this.selectedRow),
            column: "name",
          },
          this.graphqlFilter
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
      const column: string =
        item[convertToCamelCase(this.selectedColumn)].name || "not specified";
      const row: string =
        item[convertToCamelCase(this.selectedRow)].name || "not specified";

      if (!this.aggregateData[row]) {
        this.aggregateData[row] = { [column]: item.count };
      } else {
        this.aggregateData[row][column] = item.count;
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
    .map((column: IColumn) => column.name);
}
</script>

<docs>
<template>
  <demo-item>
    <label>AggregateTable</label>
    <AggregateTable
      tableId="Pet"
      schemaId="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
    />
    <label>AggregateTable with filters set</label>
    <AggregateTable
      tableId="Pet"
      schemaId="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
      :graphqlFilter="graphqlFilter"
    />
  </demo-item>
</template>

<script>
export default {
  data() {
    return {
      allColumns: [
        {
          name: "name",
          columnType: "STRING",
        },
        {
          name: "category bla",
          columnType: "REF",
        },
        {
          name: "tags",
          columnType: "ONTOLOGY_ARRAY",
        },
        {
          name: "orders",
          columnType: "REFBACK",
        },
      ],
      graphqlFilter: { name: { like: ["pooky"] } },
    };
  },
};
</script>
</docs>
